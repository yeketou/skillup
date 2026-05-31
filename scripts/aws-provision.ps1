# =============================================================================
# SkillUp — AWS Infrastructure Provisioning Script
# Region: ap-southeast-1 (Singapore)
#
# Run AFTER: aws configure (with IAM admin user credentials)
# =============================================================================

param(
    [string]$AppName    = "skillup",
    [string]$Region     = "ap-southeast-1",
    [string]$DbPassword = "",         # set via prompt if blank
    [string]$KeyPair    = ""          # EC2 SSH key pair name
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($DbPassword)) {
    $DbPassword = Read-Host -Prompt "Enter a strong password for the RDS database (skillup_app user)"
}

$Tags = "Key=Project,Value=$AppName Key=ManagedBy,Value=aws-provision-script"

Write-Host "`n=== SkillUp AWS Provisioning ===" -ForegroundColor Cyan
Write-Host "Region : $Region"
Write-Host "AppName: $AppName"
Write-Host ""

# ── 1. S3 Bucket ─────────────────────────────────────────────────────────────
Write-Host "[1/7] Creating S3 bucket..." -ForegroundColor Yellow
$BucketName = "$AppName-uploads-$(aws sts get-caller-identity --query Account --output text)"
aws s3api create-bucket `
    --bucket $BucketName `
    --region $Region `
    --create-bucket-configuration LocationConstraint=$Region | Out-Null

aws s3api put-public-access-block `
    --bucket $BucketName `
    --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

aws s3api put-bucket-versioning `
    --bucket $BucketName `
    --versioning-configuration Status=Enabled

Write-Host "    Bucket: $BucketName" -ForegroundColor Green

# ── 2. ECR Repository ─────────────────────────────────────────────────────────
Write-Host "[2/7] Creating ECR repository..." -ForegroundColor Yellow
$EcrUri = aws ecr create-repository `
    --repository-name "$AppName-backend" `
    --region $Region `
    --image-scanning-configuration scanOnPush=true `
    --query "repository.repositoryUri" `
    --output text 2>&1

if ($LASTEXITCODE -ne 0) {
    # Already exists — get the URI
    $EcrUri = aws ecr describe-repositories `
        --repository-names "$AppName-backend" `
        --region $Region `
        --query "repositories[0].repositoryUri" `
        --output text
}
Write-Host "    ECR: $EcrUri" -ForegroundColor Green

# ── 3. VPC & Networking ────────────────────────────────────────────────────────
Write-Host "[3/7] Setting up VPC & security groups..." -ForegroundColor Yellow

# Use default VPC for simplicity
$VpcId = aws ec2 describe-vpcs `
    --filters "Name=isDefault,Values=true" `
    --query "Vpcs[0].VpcId" `
    --output text `
    --region $Region

$SubnetIds = (aws ec2 describe-subnets `
    --filters "Name=vpc-id,Values=$VpcId" "Name=defaultForAz,Values=true" `
    --query "Subnets[*].SubnetId" `
    --output text `
    --region $Region) -split "`t"

Write-Host "    VPC: $VpcId  Subnets: $($SubnetIds -join ',')" -ForegroundColor Green

# Security group for EC2 (HTTP + SSH + 8088 backend + 8070 Keycloak)
$Ec2SgId = aws ec2 create-security-group `
    --group-name "$AppName-ec2-sg" `
    --description "SkillUp EC2 — HTTP, SSH, backend, Keycloak" `
    --vpc-id $VpcId `
    --region $Region `
    --query "GroupId" `
    --output text 2>&1

if ($LASTEXITCODE -ne 0) {
    $Ec2SgId = aws ec2 describe-security-groups `
        --filters "Name=group-name,Values=$AppName-ec2-sg" `
        --query "SecurityGroups[0].GroupId" `
        --output text --region $Region
} else {
    foreach ($port in @(22, 80, 8088, 8070)) {
        aws ec2 authorize-security-group-ingress `
            --group-id $Ec2SgId `
            --protocol tcp --port $port --cidr "0.0.0.0/0" `
            --region $Region | Out-Null
    }
}

# Security group for RDS (PostgreSQL — accessible from EC2 SG only)
$RdsSgId = aws ec2 create-security-group `
    --group-name "$AppName-rds-sg" `
    --description "SkillUp RDS — PostgreSQL access from EC2 only" `
    --vpc-id $VpcId `
    --region $Region `
    --query "GroupId" `
    --output text 2>&1

if ($LASTEXITCODE -ne 0) {
    $RdsSgId = aws ec2 describe-security-groups `
        --filters "Name=group-name,Values=$AppName-rds-sg" `
        --query "SecurityGroups[0].GroupId" `
        --output text --region $Region
} else {
    aws ec2 authorize-security-group-ingress `
        --group-id $RdsSgId `
        --protocol tcp --port 5432 `
        --source-group $Ec2SgId `
        --region $Region | Out-Null
}

# Security group for ElastiCache (Redis — EC2 only)
$RedisSgId = aws ec2 create-security-group `
    --group-name "$AppName-redis-sg" `
    --description "SkillUp Redis — access from EC2 only" `
    --vpc-id $VpcId `
    --region $Region `
    --query "GroupId" `
    --output text 2>&1

if ($LASTEXITCODE -ne 0) {
    $RedisSgId = aws ec2 describe-security-groups `
        --filters "Name=group-name,Values=$AppName-redis-sg" `
        --query "SecurityGroups[0].GroupId" `
        --output text --region $Region
} else {
    aws ec2 authorize-security-group-ingress `
        --group-id $RedisSgId `
        --protocol tcp --port 6379 `
        --source-group $Ec2SgId `
        --region $Region | Out-Null
}

Write-Host "    SGs: EC2=$Ec2SgId  RDS=$RdsSgId  Redis=$RedisSgId" -ForegroundColor Green

# ── 4. RDS PostgreSQL 16 ──────────────────────────────────────────────────────
Write-Host "[4/7] Creating RDS PostgreSQL 16 (db.t3.micro)..." -ForegroundColor Yellow
Write-Host "      This takes 5-10 minutes — continuing with other resources..." -ForegroundColor Gray

# RDS subnet group
aws rds create-db-subnet-group `
    --db-subnet-group-name "$AppName-subnet-group" `
    --db-subnet-group-description "SkillUp RDS subnet group" `
    --subnet-ids $SubnetIds `
    --region $Region 2>&1 | Out-Null

aws rds create-db-instance `
    --db-instance-identifier "$AppName-postgres" `
    --db-instance-class db.t3.micro `
    --engine postgres `
    --engine-version "16.3" `
    --master-username postgres `
    --master-user-password $DbPassword `
    --db-name skillup_db `
    --allocated-storage 20 `
    --storage-type gp3 `
    --storage-encrypted `
    --vpc-security-group-ids $RdsSgId `
    --db-subnet-group-name "$AppName-subnet-group" `
    --backup-retention-period 7 `
    --no-multi-az `
    --no-publicly-accessible `
    --region $Region 2>&1 | Out-Null

Write-Host "    RDS instance creation started (provisioning in background)" -ForegroundColor Green

# ── 5. ElastiCache Redis ──────────────────────────────────────────────────────
Write-Host "[5/7] Creating ElastiCache Redis (cache.t3.micro)..." -ForegroundColor Yellow

aws elasticache create-cache-subnet-group `
    --cache-subnet-group-name "$AppName-cache-subnet" `
    --cache-subnet-group-description "SkillUp cache subnet group" `
    --subnet-ids $SubnetIds `
    --region $Region 2>&1 | Out-Null

aws elasticache create-cache-cluster `
    --cache-cluster-id "$AppName-redis" `
    --cache-node-type cache.t3.micro `
    --engine redis `
    --engine-version "7.0" `
    --num-cache-nodes 1 `
    --cache-subnet-group-name "$AppName-cache-subnet" `
    --security-group-ids $RedisSgId `
    --auth-token "skillup_redis_pass" `
    --transit-encryption-enabled `
    --region $Region 2>&1 | Out-Null

Write-Host "    ElastiCache creation started (provisioning in background)" -ForegroundColor Green

# ── 6. EC2 Key Pair ────────────────────────────────────────────────────────────
Write-Host "[6/7] Creating EC2 key pair..." -ForegroundColor Yellow

$KeyPairName = "$AppName-key"
$KeyFile = "$HOME\.ssh\$KeyPairName.pem"

$existingKey = aws ec2 describe-key-pairs `
    --key-names $KeyPairName `
    --region $Region `
    --query "KeyPairs[0].KeyName" `
    --output text 2>&1

if ($LASTEXITCODE -eq 0 -and $existingKey -eq $KeyPairName) {
    Write-Host "    Key pair $KeyPairName already exists" -ForegroundColor Green
} else {
    New-Item -ItemType Directory -Force -Path "$HOME\.ssh" | Out-Null
    aws ec2 create-key-pair `
        --key-name $KeyPairName `
        --query "KeyMaterial" `
        --output text `
        --region $Region | Out-File -FilePath $KeyFile -Encoding ascii
    Write-Host "    Key saved to: $KeyFile" -ForegroundColor Green
    Write-Host "    KEEP THIS FILE SAFE — it cannot be downloaded again" -ForegroundColor Red
}

# ── 7. EC2 Instance ────────────────────────────────────────────────────────────
Write-Host "[7/7] Launching EC2 t3.medium (Amazon Linux 2023)..." -ForegroundColor Yellow

# Latest Amazon Linux 2023 AMI
$AmiId = aws ec2 describe-images `
    --owners amazon `
    --filters "Name=name,Values=al2023-ami-2023*-x86_64" `
               "Name=state,Values=available" `
    --query "sort_by(Images,&CreationDate)[-1].ImageId" `
    --output text `
    --region $Region

$UserData = @"
#!/bin/bash
yum update -y
yum install -y docker git
systemctl enable docker
systemctl start docker
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
usermod -aG docker ec2-user
mkdir -p /opt/skillup
"@
$UserDataB64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($UserData))

$InstanceId = aws ec2 run-instances `
    --image-id $AmiId `
    --instance-type t3.medium `
    --key-name $KeyPairName `
    --security-group-ids $Ec2SgId `
    --user-data $UserDataB64 `
    --block-device-mappings "DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3,DeleteOnTermination=true}" `
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$AppName-server},{Key=Project,Value=$AppName}]" `
    --region $Region `
    --query "Instances[0].InstanceId" `
    --output text

Write-Host "    EC2 Instance: $InstanceId (starting...)" -ForegroundColor Green

# Wait for instance to be running
Write-Host "    Waiting for instance to be running..." -ForegroundColor Gray
aws ec2 wait instance-running --instance-ids $InstanceId --region $Region

$PublicIp = aws ec2 describe-instances `
    --instance-ids $InstanceId `
    --query "Reservations[0].Instances[0].PublicIpAddress" `
    --output text `
    --region $Region

# ── Summary ────────────────────────────────────────────────────────────────────
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " PROVISIONING COMPLETE — save these values in your .env" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "EC2 Public IP  : $PublicIp"
Write-Host "SSH command    : ssh -i $KeyFile ec2-user@$PublicIp"
Write-Host "ECR URI        : $EcrUri"
Write-Host "S3 Bucket      : $BucketName"
Write-Host ""
Write-Host "--- Wait 5-10 min for RDS & ElastiCache, then run: ---"
Write-Host "aws rds describe-db-instances --db-instance-identifier $AppName-postgres --query 'DBInstances[0].Endpoint.Address' --output text --region $Region"
Write-Host "aws elasticache describe-cache-clusters --cache-cluster-id $AppName-redis --show-cache-node-info --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' --output text --region $Region"
Write-Host ""
Write-Host "--- Keycloak Issuer URI (update in .env after deploy) ---"
Write-Host "KEYCLOAK_ISSUER_URI=http://$PublicIp`:8070/realms/skillup"
Write-Host "KEYCLOAK_JWK_URI=http://$PublicIp`:8070/realms/skillup/protocol/openid-connect/certs"

# Save summary to file
@"
EC2_PUBLIC_IP=$PublicIp
EC2_INSTANCE_ID=$InstanceId
ECR_URI=$EcrUri
S3_BUCKET=$BucketName
KEY_FILE=$KeyFile
REGION=$Region
"@ | Out-File -FilePath "$PSScriptRoot\aws-resources.txt" -Encoding utf8

Write-Host "`nSaved to scripts\aws-resources.txt" -ForegroundColor Green
