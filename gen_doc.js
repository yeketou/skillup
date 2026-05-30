const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, LevelFormat,
  BorderStyle, WidthType, ShadingType, PageNumber, TableOfContents,
  PageBreak
} = require('docx');
const fs = require('fs');

// ── Colour palette ───────────────────────────────────────────────
const C = {
  indigo:     '4F46E5',
  indigoDark: '3730A3',
  purple:     '7C3AED',
  navy:       '1E1B4B',
  success:    '059669',
  warning:    'D97706',
  danger:     'DC2626',
  resolved:   'D1FAE5',  // green tint for resolved decisions
  tblHead:    'EEF2FF',
  tblAlt:     'F9FAFB',
  border:     'C7D2FE',
};

// ── Helpers ──────────────────────────────────────────────────────
const hr = () => new Paragraph({
  border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: C.indigo, space: 1 } },
  spacing: { after: 200 }, children: []
});
const spacer = (pt = 120) => new Paragraph({ spacing: { before: pt, after: 0 }, children: [] });
const body  = (text, opts = {}) => new Paragraph({
  spacing: { before: 60, after: 120 },
  children: [new TextRun({ text, font: 'Arial', size: 22, ...opts })]
});
const bodyRuns = (runs, opts = {}) => new Paragraph({
  spacing: { before: 60, after: 120 }, ...opts,
  children: runs.map(r => typeof r === 'string'
    ? new TextRun({ text: r, font: 'Arial', size: 22 })
    : new TextRun({ font: 'Arial', size: 22, ...r }))
});
const bullet   = (text, lvl = 0) => new Paragraph({
  numbering: { reference: 'bullets', level: lvl },
  spacing: { before: 40, after: 60 },
  children: [new TextRun({ text, font: 'Arial', size: 22 })]
});
const numbered = (text, lvl = 0) => new Paragraph({
  numbering: { reference: 'numbers', level: lvl },
  spacing: { before: 40, after: 60 },
  children: [new TextRun({ text, font: 'Arial', size: 22 })]
});
const h1 = t => new Paragraph({
  heading: HeadingLevel.HEADING_1, pageBreakBefore: true,
  spacing: { before: 0, after: 200 },
  children: [new TextRun({ text: t, font: 'Arial', size: 36, bold: true, color: C.navy })]
});
const h2 = t => new Paragraph({
  heading: HeadingLevel.HEADING_2,
  spacing: { before: 280, after: 120 },
  children: [new TextRun({ text: t, font: 'Arial', size: 28, bold: true, color: C.indigo })]
});
const h3 = t => new Paragraph({
  heading: HeadingLevel.HEADING_3,
  spacing: { before: 200, after: 80 },
  children: [new TextRun({ text: t, font: 'Arial', size: 24, bold: true, color: C.navy })]
});
const h4 = t => new Paragraph({
  spacing: { before: 160, after: 60 },
  children: [new TextRun({ text: t, font: 'Arial', size: 22, bold: true, underline: {} })]
});

// ── Decision callout box (green) ─────────────────────────────────
const decision = (label, text) => new Paragraph({
  spacing: { before: 120, after: 120 },
  indent: { left: 720, right: 360 },
  shading: { fill: 'D1FAE5', type: ShadingType.CLEAR },
  border: { left: { style: BorderStyle.SINGLE, size: 16, color: C.success, space: 6 } },
  children: [
    new TextRun({ text: `✓ DECISION: `, font: 'Arial', size: 20, bold: true, color: C.success }),
    new TextRun({ text: label, font: 'Arial', size: 20, bold: true, color: '065F46' }),
    new TextRun({ text: `  —  ${text}`, font: 'Arial', size: 20, color: '065F46' }),
  ]
});

// ── Table helpers ────────────────────────────────────────────────
const brd = { style: BorderStyle.SINGLE, size: 1, color: C.border };
const borders = { top: brd, bottom: brd, left: brd, right: brd };
const hCell = (text, w) => new TableCell({
  width: { size: w, type: WidthType.DXA },
  shading: { fill: C.tblHead, type: ShadingType.CLEAR },
  borders: { top: brd, bottom: { style: BorderStyle.SINGLE, size: 2, color: C.indigo }, left: brd, right: brd },
  margins: { top: 80, bottom: 80, left: 120, right: 120 },
  children: [new Paragraph({ children: [new TextRun({ text, font: 'Arial', size: 20, bold: true, color: C.navy })] })]
});
const dCell = (text, w, shade = 'FFFFFF') => new TableCell({
  width: { size: w, type: WidthType.DXA },
  shading: { fill: shade, type: ShadingType.CLEAR },
  borders, margins: { top: 80, bottom: 80, left: 120, right: 120 },
  children: [new Paragraph({ children: [new TextRun({ text, font: 'Arial', size: 20 })] })]
});
const dataTable = (headers, rows, colWidths) => {
  const total = colWidths.reduce((a, b) => a + b, 0);
  return new Table({
    width: { size: total, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: [
      new TableRow({ tableHeader: true, children: headers.map((h, i) => hCell(h, colWidths[i])) }),
      ...rows.map((row, ri) => new TableRow({
        children: row.map((val, ci) => dCell(val, colWidths[ci], ri % 2 === 0 ? 'FFFFFF' : C.tblAlt))
      }))
    ]
  });
};
const twoCol = (rows, colW = [3000, 6360]) => new Table({
  width: { size: 9360, type: WidthType.DXA }, columnWidths: colW,
  rows: rows.map((r, i) => new TableRow({
    children: [
      new TableCell({
        width: { size: colW[0], type: WidthType.DXA },
        shading: { fill: i === 0 ? C.tblHead : 'FFFFFF', type: ShadingType.CLEAR },
        borders: { top: brd, bottom: brd, left: brd, right: brd },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: r[0], font: 'Arial', size: 20, bold: i === 0 })] })]
      }),
      new TableCell({
        width: { size: colW[1], type: WidthType.DXA },
        shading: { fill: i === 0 ? C.tblHead : (i % 2 === 0 ? C.tblAlt : 'FFFFFF'), type: ShadingType.CLEAR },
        borders: { top: brd, bottom: brd, left: brd, right: brd },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        children: [new Paragraph({ children: [new TextRun({ text: r[1], font: 'Arial', size: 20, bold: i === 0 })] })]
      })
    ]
  }))
});

// ═══════════════════════════════════════════════════════════════
//  COVER PAGE
// ═══════════════════════════════════════════════════════════════
const coverPage = [
  spacer(1440),
  new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 0, after: 160 },
    children: [new TextRun({ text: 'SKILLUP', font: 'Arial', size: 80, bold: true, color: C.indigo })]
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 0, after: 80 },
    children: [new TextRun({ text: 'Tuition Management System', font: 'Arial', size: 36, color: C.navy })]
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 0, after: 600 },
    children: [new TextRun({ text: 'Functional Specification Document', font: 'Arial', size: 28, italics: true, color: '6B7280' })]
  }),
  hr(),
  spacer(200),
  twoCol([
    ['Field',            'Details'],
    ['Document Version', '1.1'],
    ['Date',             'May 2026'],
    ['Status',           'Updated — Client Decisions Incorporated'],
    ['Prepared by',      'SkillUp Product Team'],
    ['Target Audience',  'Tuition Centre Owners, Teachers, Developers'],
    ['Platform',         'Web Application + Mobile Portal (iOS / Android)'],
    ['Change from v1.0', '10 open questions resolved; 7 affected sections updated'],
  ]),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 1 — Introduction
// ═══════════════════════════════════════════════════════════════
const sec1 = [
  h1('1.  Introduction'),
  h2('1.1  Purpose of This Document'),
  body('This Functional Specification Document (FSD) defines the complete set of features, behaviours, and business rules for the SkillUp Tuition Management System. It serves as the primary reference for designers, developers, testers, and stakeholders throughout the software development lifecycle.'),
  body('Version 1.1 incorporates all ten client decisions that were previously listed as Open Questions in v1.0. Affected sections have been updated in-place; a full Decision Log is provided in Section 12.'),

  h2('1.2  Background'),
  body('Tuition centres in Malaysia typically operate with manual or semi-automated systems — spreadsheets for fee tracking, WhatsApp broadcasts for communication, and paper registers for attendance. This creates administrative overhead and limits visibility for parents and students.'),
  body('SkillUp is designed to eliminate these inefficiencies by providing a single, integrated platform that automates routine tasks, delivers real-time information to all stakeholders, and generates actionable reports for centre management.'),

  h2('1.3  Scope'),
  body('Version 1.0 covers the following functional areas:'),
  bullet('Student registration and profile management'),
  bullet('Class and timetable management (multi-branch)'),
  bullet('Teacher-led attendance marking with optional QR self-check-in and automated parent notifications'),
  bullet('Monthly fee billing, payment tracking (manual recording), and receipt generation'),
  bullet('Assignment creation with student file-upload capability'),
  bullet('Academic result recording and performance analytics'),
  bullet('WhatsApp Business API-based parent communication with 3-day advance fee reminders'),
  bullet('Management reporting dashboard (branch-level and aggregate)'),
  bullet('Student and parent shared self-service portal with dark mode toggle'),

  h2('1.4  Definitions and Abbreviations'),
  dataTable(
    ['Term', 'Definition'],
    [
      ['FSD',            'Functional Specification Document'],
      ['Admin',          'Centre or branch administrator with full system access'],
      ['Platform Admin', 'Super-admin with access to all branches and global settings'],
      ['Teacher',        'Subject teacher with access limited to their classes'],
      ['Parent',         'Guardian registered against one or more students'],
      ['Student',        'Enrolled learner; shares a single portal account with their parent'],
      ['Branch',         'An individual physical location operated under one SkillUp account'],
      ['Form',           'Malaysian secondary school year level (Form 1–5)'],
      ['FPX',            'Financial Process Exchange — Malaysian online banking gateway (v2+)'],
      ['DuitNow',        'Real-time payment network in Malaysia (v2+)'],
      ['WhatsApp API',   'Official WhatsApp Business API used for automated outbound messaging'],
      ['OTP',            'One-Time Password for authentication'],
      ['QR Check-in',    'Optional QR-code-based student self-check-in at class start'],
    ],
    [2000, 7360]
  ),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 2 — System Overview
// ═══════════════════════════════════════════════════════════════
const sec2 = [
  h1('2.  System Overview'),
  h2('2.1  System Description'),
  body('SkillUp is a cloud-hosted, multi-branch, multi-role web and mobile application. The system is organised into two distinct portals:'),
  bullet('Admin Portal — a desktop-optimised web interface for administrators and teachers to manage all operational activities across one or more branches.'),
  bullet('Student Portal — a mobile-first web/app interface for students and parents to view records, schedule, attendance, fees, and assignments. Students and parents share one account.'),

  h2('2.2  User Roles and Access Levels'),
  dataTable(
    ['Role', 'Portal', 'Access Level', 'Typical User'],
    [
      ['Platform Admin', 'Admin Portal', 'Full — all branches, global settings, cross-branch reports', 'Centre owner / group manager'],
      ['Branch Admin',   'Admin Portal', 'Full — own branch only; cannot access other branches',        'Branch manager'],
      ['Teacher',        'Admin Portal', 'Limited — mark attendance, manage own classes and assignments', 'Subject teacher'],
      ['Student/Parent', 'Student Portal','Read-only — own student\'s records only (shared account)',    'Student or parent'],
    ],
    [1600, 2000, 3360, 2400]
  ),

  h2('2.3  Multi-Branch Architecture'),
  decision('Multi-branch', 'The system supports multiple branches under a single SkillUp account.'),
  body('Each branch is an independent operational unit with its own students, classes, teachers, rooms, and financial records. The hierarchy is:'),
  bullet('Platform (account root) — one per tuition centre group, holds global settings and Platform Admin users.'),
  bullet('Branch — one per physical location (e.g., "Main Centre, Taman Desa" and "Branch 2, Kepong"). Branches are independent; student records do not automatically transfer between branches.'),
  bullet('Class Session — belongs to exactly one branch. A teacher may be assigned to classes across multiple branches.'),
  body('Access control by branch:'),
  bullet('A Platform Admin can view and manage all branches from a single login. Reports can be filtered per branch or aggregated across all branches.'),
  bullet('A Branch Admin can only view and manage their assigned branch.'),
  bullet('A Teacher assigned to multiple branches can switch between branch contexts from the same login.'),
  body('Branch-specific settings (configurable per branch): centre name, address, fee structures, WhatsApp sender ID, and receipt letterhead.'),

  h2('2.4  High-Level Architecture'),
  body('The system follows a three-tier architecture:'),
  bullet('Presentation Layer: Responsive web application (Admin Portal) and mobile-first web app (Student Portal), both accessible via any modern browser.'),
  bullet('Application Layer: RESTful API built with Spring Boot 3.x (Java 21), handling all business logic and data processing.'),
  bullet('Data Layer: PostgreSQL 16 relational database with schema-per-branch isolation.'),
  body('Supporting infrastructure:'),
  bullet('Redis — session caching and rate limiting'),
  bullet('Kafka — asynchronous event processing (absence notifications, reminder scheduling)'),
  bullet('WhatsApp Business API (official) — outbound messaging to parents'),
  bullet('Cloud object storage — assignment file uploads, receipt PDFs, profile photos'),

  h2('2.5  Key Design Principles'),
  bullet('Mobile-first Student Portal: Students and parents primarily access the system on smartphones.'),
  bullet('Automation-first: Fee reminders, absence alerts, and monthly reports are automated.'),
  bullet('English-only UI: All screens are in English. Automated WhatsApp messages are composed in Bahasa Malaysia by default.'),
  bullet('Offline-tolerant: The Admin Portal degrades gracefully under poor connectivity; attendance can be queued and synced.'),
  bullet('Branch isolation: Each branch\'s data is logically isolated; no accidental cross-branch access.'),
  bullet('Manual payments in v1.0: Online payment gateway integration is deferred to Version 2.0.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 3 — Student Management
// ═══════════════════════════════════════════════════════════════
const sec3 = [
  h1('3.  Student Management Module'),
  h2('3.1  Overview'),
  body('The Student Management Module is the core data registry. It maintains complete records for every enrolled student, including personal details, parent/guardian information, subject enrolments, and current status. Student records are branch-scoped — a student enrolled at Branch A is not visible to Branch B administrators.'),

  h2('3.2  Student Registration'),
  h3('3.2.1  Registering a New Student'),
  body('An Admin registers a new student by completing the Registration Form. The following fields are captured:'),

  h4('Student Information'),
  dataTable(
    ['Field', 'Type', 'Required', 'Notes'],
    [
      ['Full Name',      'Text',     'Yes', 'As per IC / birth certificate'],
      ['IC Number',      'Text',     'Yes', 'Format: YYMMDD-PP-NNNN; unique within the branch'],
      ['Date of Birth',  'Date',     'Yes', 'Auto-derived from IC for Malaysian IC format'],
      ['Form / Year',    'Dropdown', 'Yes', 'Form 1 to Form 5'],
      ['School Name',    'Text',     'Yes', 'Day school the student attends'],
      ['Gender',         'Dropdown', 'Yes', 'Male / Female'],
      ['Profile Photo',  'Image',    'No',  'JPEG or PNG, max 2 MB'],
      ['Medical Notes',  'Text',     'No',  'Relevant conditions for attendance records'],
    ],
    [2200, 1400, 1200, 4560]
  ), spacer(80),

  h4('Parent / Guardian Information'),
  decision('Single shared account', 'Students and parents share one portal account, created by Admin and linked to the student record.'),
  dataTable(
    ['Field', 'Type', 'Required', 'Notes'],
    [
      ['Parent Full Name',   'Text',  'Yes', 'Primary contact person'],
      ['Relationship',       'Dropdown','Yes','Father / Mother / Guardian'],
      ['WhatsApp Number',    'Phone', 'Yes', 'Used for automated notifications; must include country code (+60)'],
      ['Email Address',      'Email', 'Yes', 'Used for portal account login and receipt delivery'],
      ['Secondary Contact',  'Phone', 'No',  'Backup emergency number'],
      ['Secondary Name',     'Text',  'No',  'Name of secondary contact'],
    ],
    [2400, 1400, 1200, 4360]
  ), spacer(80),

  body('On registration, the system:'),
  numbered('Assigns a unique Student ID (format: STU-YYYY-NNNN, scoped to the branch).'),
  numbered('Creates a shared Student Portal account using the parent\'s email as the username and a system-generated temporary password.'),
  numbered('Sends a WhatsApp welcome message to the parent containing the portal URL and login credentials.'),
  numbered('Records the registration fee as the first payment transaction.'),
  numbered('Enrols the student in the selected subject classes.'),

  h3('3.2.2  Sibling Discount'),
  body('If a second or subsequent child from the same family is enrolled at the same branch, the Admin can apply a sibling discount (default: 10% off the monthly fee). Siblings are linked by matching the parent\'s phone number or IC number.'),

  h2('3.3  Student Directory'),
  body('The Student Directory shows a searchable, paginated list of all students in the current branch. Platform Admins can switch the branch context via a dropdown in the top bar. Filters available: Form, Subject, Status, and Fee Status.'),

  h2('3.4  Student Profile'),
  dataTable(
    ['Tab', 'Contents'],
    [
      ['Overview',    'Personal info, parent contact, enrollment date, fee summary, quick actions'],
      ['Attendance',  'Monthly calendar, subject breakdown, at-risk flag'],
      ['Academic',    'Exam results, performance trend chart'],
      ['Assignments', 'Tasks with submission status, uploaded files, and scores'],
      ['Fees',        'Payment history, outstanding balance, receipt download'],
      ['Notes',       'Admin-only internal notes'],
    ],
    [1800, 7560]
  ),

  h2('3.5  Student Status'),
  dataTable(
    ['Status', 'Meaning', 'Triggered By'],
    [
      ['Active',    'Currently enrolled and attending',                        'Default on registration'],
      ['Inactive',  'Has stopped attending; record retained',                  'Admin marks inactive'],
      ['At Risk',   'Attendance below configured threshold in current month',  'System auto-flags'],
      ['Graduated', 'Completed Form 5; record archived',                       'Admin marks graduated'],
    ],
    [1600, 4200, 3560]
  ),

  h2('3.6  Business Rules'),
  bullet('Student records cannot be hard-deleted. Inactive/Graduated records are archived to preserve history.'),
  bullet('IC Number must be unique within a branch.'),
  bullet('At least one subject must be enrolled at registration.'),
  bullet('An At Risk flag triggers a WhatsApp notification to the parent (sent once per threshold crossing per month).'),
  bullet('A student\'s portal account (email + password) belongs to the parent; the student logs in using the same credentials.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 4 — Class Management
// ═══════════════════════════════════════════════════════════════
const sec4 = [
  h1('4.  Class Management Module'),
  h2('4.1  Overview'),
  body('The Class Management Module defines the centre\'s subject offerings, classroom resources, weekly timetable, and scheduling rules. All class data is branch-scoped.'),

  h2('4.2  Subject Catalogue'),
  body('Admins maintain a catalogue of subjects per branch. Each subject contains:'),
  dataTable(
    ['Field', 'Description'],
    [
      ['Subject Name',   'e.g., Mathematics, Add Mathematics, Science, English, BM, Physics, Chemistry'],
      ['Subject Code',   'Short code for reports (e.g., MATH, ADDM, SCI)'],
      ['Monthly Fee',    'Default fee per student per month for this subject'],
      ['Target Forms',   'Form levels this subject is available for'],
      ['Max Class Size', 'Maximum number of students per session'],
    ],
    [2400, 6960]
  ),

  h2('4.3  Class Sessions'),
  dataTable(
    ['Field', 'Type', 'Notes'],
    [
      ['Subject',        'Dropdown',    'From the branch subject catalogue'],
      ['Form Group',     'Dropdown',    'Form 1–5 or mixed'],
      ['Teacher',        'Dropdown',    'Assigned teacher; may be from any branch'],
      ['Day of Week',    'Multi-select','Monday–Sunday'],
      ['Start Time',     'Time',        '24-hour format'],
      ['Duration',       'Number',      'Session length in minutes'],
      ['Classroom',      'Dropdown',    'Room from the branch room registry'],
      ['Max Students',   'Number',      'Overrides subject default for this session'],
      ['Effective From', 'Date',        'Timetable entry start date'],
      ['Effective To',   'Date',        'Optional end date'],
    ],
    [2000, 1600, 5760]
  ),

  h2('4.4  Timetable View'),
  body('A weekly grid view: rows are time bands (8 AM–6 PM), columns are weekdays. Each cell shows subject, teacher, room, and enrolment count vs capacity. Cells are colour-coded by subject. Branch Admins see only their branch; Platform Admins can view all branches.'),

  h2('4.5  Classroom Registry'),
  body('Each branch maintains its own room registry. Fields: Room name, Capacity, Room type (Classroom / Lab / Hall). Double-booking prevention is enforced — the same room cannot be assigned to two concurrent sessions within the same branch.'),

  h2('4.6  Holiday and Replacement Classes'),
  dataTable(
    ['Field', 'Description'],
    [
      ['Cancellation Date', 'The original class date being cancelled'],
      ['Affected Classes',  'One specific session or all sessions on that date'],
      ['Reason',            'Public Holiday / Teacher Leave / Centre Closure / Other'],
      ['Replacement Date',  'Optional make-up class date (clash detection enabled)'],
      ['Replacement Room',  'Room for the makeup class'],
      ['Notify Parents',    'Triggers WhatsApp notification if checked'],
    ],
    [2400, 6960]
  ),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 5 — Attendance
// ═══════════════════════════════════════════════════════════════
const sec5 = [
  h1('5.  Attendance Management Module'),
  h2('5.1  Overview'),
  body('Attendance is recorded by the teacher at the start of each class. The system provides two methods: teacher-led marking (primary) and QR-based student self-check-in (optional supplement). Absence notifications are automatically dispatched via WhatsApp.'),

  h2('5.2  Primary Method — Teacher-Led Marking'),
  body('The teacher selects the class session and date. The system presents the student roster. For each student the teacher records one of three statuses:'),
  dataTable(
    ['Status', 'Code', 'Meaning', 'Auto-notification?'],
    [
      ['Present', 'P', 'Attended the full session',       'No'],
      ['Absent',  'A', 'Did not attend',                  'Yes — WhatsApp to parent immediately'],
      ['Late',    'L', 'Arrived after session started',   'No (configurable to notify)'],
    ],
    [1400, 800, 4360, 2800]
  ),
  body('An optional note can be added per student (e.g., "medical certificate provided"). Submissions can be corrected within 24 hours by the teacher or at any time by an Admin.'),

  h2('5.3  Optional Method — QR Code Self-Check-In'),
  decision('QR check-in', 'QR-based self-check-in is included as an optional supplement to teacher-led marking.'),
  body('When enabled for a class session, the teacher generates a session QR code from the Attendance screen. The QR code:'),
  bullet('Is unique per session instance (date + class).'),
  bullet('Expires 15 minutes after the session start time.'),
  bullet('Can be displayed on a classroom screen, printed, or shown on the teacher\'s phone.'),
  body('To check in, the student scans the QR code using the Student Portal app (camera permission required). A successful scan records the student as Present with a timestamp.'),
  body('QR check-in rules:'),
  bullet('Scanning after the 15-minute window records the student as Late, not Present.'),
  bullet('Students who do not scan within the window are not automatically marked Absent — the teacher reviews and completes the roster for any remaining students.'),
  bullet('A teacher can override any QR-generated status at any time before submission.'),
  bullet('QR check-in can be enabled or disabled per session by the teacher; it is off by default.'),

  h2('5.4  Automated Absence Notification'),
  body('When a student is marked Absent and the record is saved, the system sends a WhatsApp message to the parent via the WhatsApp Business API. Template (Bahasa Malaysia default):'),
  spacer(80),
  new Paragraph({
    spacing: { before: 80, after: 80 },
    indent: { left: 720, right: 720 },
    shading: { fill: 'ECF8F1', type: ShadingType.CLEAR },
    border: { left: { style: BorderStyle.SINGLE, size: 12, color: '25D366', space: 4 } },
    children: [
      new TextRun({ text: 'Salam, Pn./En. [Parent Name] ', font: 'Arial', size: 20, bold: true }),
      new TextRun({ text: '\nAnak tuan/puan, ', font: 'Arial', size: 20 }),
      new TextRun({ text: '[Student Name]', font: 'Arial', size: 20, bold: true }),
      new TextRun({ text: ', tidak hadir ke kelas ', font: 'Arial', size: 20 }),
      new TextRun({ text: '[Subject Name]', font: 'Arial', size: 20, bold: true }),
      new TextRun({ text: ' hari ini ([Date]).\n\nJika ada sebarang pertanyaan, sila hubungi kami.\n', font: 'Arial', size: 20 }),
      new TextRun({ text: '[Centre Name]  |  [Centre Phone]', font: 'Arial', size: 20, italics: true }),
    ]
  }),
  spacer(120),

  h2('5.5  Attendance Calculations'),
  new Paragraph({
    alignment: AlignmentType.CENTER, spacing: { before: 120, after: 120 },
    children: [new TextRun({ text: 'Attendance % = (Sessions Present + Sessions Late) / Total Sessions × 100', font: 'Courier New', size: 22, bold: true, color: C.indigo })]
  }),
  body('Calculated per subject and as an overall percentage. Updated in real time on submission.'),

  h2('5.6  At-Risk Threshold'),
  body('A student whose overall attendance in the current month falls below 70% (configurable) is flagged At Risk. The flag appears on the Student Directory and triggers a one-time WhatsApp notification to the parent per threshold crossing.'),

  h2('5.7  Attendance Reports'),
  bullet('Per Student — monthly calendar and subject breakdown (visible to Admin, Teacher, and via Student Portal).'),
  bullet('Per Class — session-by-session roster for a selected period.'),
  bullet('Centre-wide / Branch aggregate — attendance rates by form, subject, and teacher for a selected month.'),
  bullet('Platform Admin — cross-branch attendance comparison report.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 6 — Fees & Billing
// ═══════════════════════════════════════════════════════════════
const sec6 = [
  h1('6.  Fees and Billing Module'),
  h2('6.1  Overview'),
  body('The Fees and Billing Module manages all financial transactions between the tuition centre and students\' families. Version 1.0 uses manual payment recording only; online payment gateway integration (FPX / DuitNow) is planned for Version 2.0.'),
  decision('Manual recording only', 'Online payment integration (FPX / DuitNow QR) is deferred to Version 2.0. All payments in v1.0 are recorded manually by Admin.'),

  h2('6.2  Fee Structures'),
  dataTable(
    ['Fee Type', 'Description', 'Billing Cycle'],
    [
      ['Monthly Tuition Fee',   'Per-subject monthly charge; set in the subject catalogue',          'Monthly'],
      ['Registration Fee',      'One-time fee on first enrolment; default RM 50',                    'One-time'],
      ['Sibling Discount',      'Percentage reduction for 2nd+ siblings; default 10%',               'Monthly'],
      ['Custom Adjustment',     'Admin can manually override the fee for individual students',        'Monthly'],
      ['Exam Material Fee',     'Optional supplementary charge for exam papers, worksheets, etc.',   'Ad hoc'],
    ],
    [2600, 4400, 2360]
  ),

  h2('6.3  Monthly Invoice Generation'),
  body('On the first day of each calendar month, the system automatically generates a fee record for every active student in every branch. The record itemises each subject\'s fee and applicable discounts. Invoice generation does not send notifications immediately; reminders follow the schedule in Section 6.6.'),

  h2('6.4  Recording Payments'),
  dataTable(
    ['Field', 'Description'],
    [
      ['Student',         'The student the payment applies to'],
      ['Payment Month(s)','The month(s) being settled'],
      ['Amount Received', 'Actual amount received'],
      ['Payment Method',  'Cash / Bank Transfer / DuitNow (manual confirmation) / Cheque'],
      ['Reference No.',   'Bank transaction reference (for non-cash payments)'],
      ['Date Received',   'Date of payment; defaults to today'],
      ['Notes',           'e.g., "partial payment, balance RM 60"'],
    ],
    [2400, 6960]
  ),
  body('Partial payments are supported. The system tracks the remaining balance and maintains a partial-paid status until the full amount is settled.'),

  h2('6.5  Outstanding Fee Tracking'),
  dataTable(
    ['State', 'Condition', 'Display'],
    [
      ['Paid',        'Full fee received for the current month',                 'Green badge'],
      ['Partial',     'Payment received but less than total due',                'Orange badge + remaining amount'],
      ['Overdue',     'Prior month fee not fully settled',                       'Red badge + months overdue count'],
      ['Not Yet Due', 'Current month invoice exists; due date not reached',      'Grey badge'],
    ],
    [1600, 4400, 3360]
  ),

  h2('6.6  Payment Reminder Schedule'),
  decision('3-day advance reminder', 'An automatic WhatsApp reminder is sent 3 days before the monthly due date, in addition to the standard on-month reminders.'),
  dataTable(
    ['Trigger', 'Day', 'Recipients', 'Message Type'],
    [
      ['Advance notice',        '3 days before due date (e.g., 28th of month)', 'All parents with upcoming fees',   'Friendly advance reminder — amount, due date, payment details'],
      ['First reminder',        'Day 5 of the month',                           'Parents of unpaid students',       'First fee reminder — amount due, subjects, payment options'],
      ['Second reminder',       'Day 15 of the month',                          'Parents still unpaid',             'Second reminder — stronger tone, outstanding amount'],
      ['Overdue notice',        'Day 1 of following month',                     'Parents with overdue balance',     'Overdue notice — months outstanding, request for payment'],
      ['Manual (ad hoc)',       'Any time, triggered by Admin',                 'Admin-selected recipients',        'Admin-chosen template or custom message'],
    ],
    [2200, 2800, 2400, 1960]
  ),
  body('The due date is configurable per branch (default: 1st of the month). The 3-day advance reminder date is automatically calculated from the configured due date.'),

  h2('6.7  Receipt Generation'),
  body('Receipts are generated automatically on payment recording. Each receipt (PDF) contains: SkillUp logo and branch name, unique receipt number, student name and ID, payment month(s), itemised subject fees and discounts, total paid, payment method, date, and the Admin\'s name. Receipts can be printed or shared as PDF via WhatsApp to the parent.'),

  h2('6.8  Business Rules'),
  bullet('All payments are recorded manually in Version 1.0. The system does not auto-confirm bank transfers.'),
  bullet('Fees cannot be backdated beyond 6 months without Platform Admin approval.'),
  bullet('A student with 3+ months of outstanding fees is flagged; the system recommends (does not enforce) suspension.'),
  bullet('All monetary values are stored in Malaysian Ringgit (RM) to 2 decimal places.'),
  bullet('Fee structures are branch-specific; different branches may charge different rates for the same subject.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 7 — Communications
// ═══════════════════════════════════════════════════════════════
const sec7 = [
  h1('7.  Parent Communication Module'),
  h2('7.1  WhatsApp Business API Integration'),
  decision('WhatsApp Business API', 'The official WhatsApp Business API (Meta) is the required messaging channel. Third-party wrappers are not permitted for v1.0.'),
  body('All automated and manual outbound messages are delivered via the official WhatsApp Business API. Each branch registers its own WhatsApp Business phone number. Key API requirements:'),
  bullet('A verified WhatsApp Business Account (WABA) is required per branch phone number.'),
  bullet('Template messages (for automated notifications) must be approved by Meta before use. Approval typically takes 24–48 hours.'),
  bullet('Free-form messages (for manual messaging) may only be sent within 24 hours of the last incoming message from that contact. Automated notifications use pre-approved templates exclusively.'),
  bullet('The system queues messages and retries failed deliveries up to 3 times with a 5-minute interval.'),
  bullet('All messages are logged with delivery status (Sent / Delivered / Read / Failed).'),

  h2('7.2  Automated Notification Types'),
  dataTable(
    ['Notification',               'Trigger',                              'Recipients',                     'Template Required?'],
    [
      ['Welcome Message',          'New student registration',              'Parent of new student',           'Yes'],
      ['Absence Alert',            'Student marked Absent',                 'Parent(s) of absent student',     'Yes'],
      ['At-Risk Warning',          'Attendance drops below threshold',       'Parent(s) of at-risk student',    'Yes'],
      ['3-Day Advance Fee Reminder','3 days before branch due date',         'All parents with upcoming fees',  'Yes'],
      ['First Fee Reminder',       'Day 5 of month',                        'Parents of unpaid students',      'Yes'],
      ['Second Fee Reminder',      'Day 15 of month',                       'Parents still unpaid',            'Yes'],
      ['Overdue Fee Notice',       'Day 1 of following month',              'Parents with overdue balance',    'Yes'],
      ['Payment Confirmation',     'Admin records a payment',               'Paying parent',                   'Yes'],
      ['Class Cancellation',       'Admin records a cancellation',          'All affected parents',            'Yes'],
      ['Replacement Class',        'Admin schedules a replacement',         'All affected parents',            'Yes'],
      ['New Assignment',           'Teacher creates an assignment',         'Parents of enrolled students',    'Yes'],
    ],
    [3000, 2400, 2200, 1760]
  ),

  h2('7.3  Manual Messaging'),
  body('Admins can send ad hoc WhatsApp messages at any time. Recipient options: All Parents in Branch / Parents with Outstanding Fees / Parents of Today\'s Absent Students / Specific Student. A preview is rendered with all variable substitutions before sending. Bulk sends are queued to respect WhatsApp API rate limits.'),

  h2('7.4  Message Templates'),
  dataTable(
    ['Variable',         'Replaced With'],
    [
      ['[Parent Name]',     'Parent\'s full name'],
      ['[Student Name]',   'Student\'s full name'],
      ['[Subject Name]',   'Subject class name'],
      ['[Date]',           'Relevant date in DD/MM/YYYY format'],
      ['[Amount]',         'Fee amount in RM (e.g., RM 240.00)'],
      ['[Due Date]',       'Monthly fee due date for the branch'],
      ['[Months Overdue]', 'Number of months with outstanding balance'],
      ['[Centre Name]',    'Branch registered name'],
      ['[Centre Phone]',   'Branch WhatsApp contact number'],
      ['[Payment Details]','Bank account number and DuitNow ID'],
    ],
    [2400, 6960]
  ),

  h2('7.5  Notification Log'),
  body('Every outbound message is logged with: timestamp, branch, recipient, template used, delivery status, and the triggering Admin (for manual messages). The log is searchable and filterable by date range, branch, and delivery status.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 8 — Reports
// ═══════════════════════════════════════════════════════════════
const sec8 = [
  h1('8.  Reports Module'),
  h2('8.1  Overview'),
  body('Pre-built management reports are accessible to Admins and Platform Admins. All reports support branch filtering; Platform Admins can also view cross-branch aggregates. Reports can be viewed on screen, exported to PDF, or exported to Excel.'),

  h2('8.2  Available Reports'),
  dataTable(
    ['Report Name', 'Description', 'Branch Filter', 'Frequency'],
    [
      ['Monthly Revenue Summary',   'Fees collected vs invoiced, broken down by subject',       'Per branch / All branches', 'Monthly'],
      ['Outstanding Fees',          'All students with unpaid or partial fees',                  'Per branch',                'On demand'],
      ['Student Attendance',        'Attendance % per student for a selected month/subject',     'Per branch',                'Monthly / On demand'],
      ['Class Attendance Roster',   'Session-by-session roster for a specific class',            'Per branch',                'On demand'],
      ['At-Risk Students',          'Students flagged At Risk in the current period',            'Per branch / All branches', 'Weekly / On demand'],
      ['Enrolment Trend',           'New enrolments and withdrawals per month',                  'Per branch / All branches', 'Monthly'],
      ['Student Performance',       'Academic results summary with improvement trend',           'Per branch',                'Per exam cycle'],
      ['Assignment Completion',     'Submission rates per assignment and per class',             'Per branch',                'On demand'],
      ['Revenue by Subject',        'Monthly fees by subject taught',                           'Per branch / All branches', 'Monthly'],
      ['Cross-Branch Comparison',   'KPI comparison across all branches (Platform Admin only)', 'All branches',              'Monthly'],
      ['WhatsApp Delivery Report',  'All messages sent with delivery status summary',            'Per branch',                'Monthly'],
    ],
    [2600, 3200, 1760, 1800]
  ),

  h2('8.3  Dashboard'),
  body('The Admin Dashboard provides real-time summary widgets: KPI cards, quick-action buttons, monthly revenue chart, today\'s class schedule, outstanding fees table, and recent activity feed. Branch Admins see their branch data; Platform Admins see a combined view with a branch selector.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 9 — Assignments & Academic Records
// ═══════════════════════════════════════════════════════════════
const sec9 = [
  h1('9.  Assignments and Academic Records Module'),
  h2('9.1  Assignment Management'),
  h3('9.1.1  Creating an Assignment'),
  dataTable(
    ['Field', 'Required', 'Description'],
    [
      ['Title',            'Yes', 'Descriptive name (e.g., "Math Worksheet 5A")'],
      ['Class',            'Yes', 'The class session this assignment applies to'],
      ['Due Date',         'Yes', 'Date by which students must submit'],
      ['Maximum Score',    'No',  'If scored, the maximum achievable mark (e.g., 100)'],
      ['Description',      'No',  'Instructions or notes for students; visible in Student Portal'],
      ['Allow File Upload','No',  'Checkbox enabling students to submit PDF/image files via the portal'],
      ['Notify Parents',   'No',  'Sends a WhatsApp notification when the assignment is created'],
    ],
    [2200, 1400, 5760]
  ),

  h3('9.1.2  Student File Submission (Student Portal)'),
  decision('File upload required', 'Students can upload assignment files (PDF or image) via the Student Portal.'),
  body('When "Allow File Upload" is enabled on an assignment, students and parents can attach files through the Student Portal:'),
  bullet('Accepted file types: PDF, JPEG, PNG.'),
  bullet('Maximum file size: 10 MB per file; maximum 3 files per assignment submission.'),
  bullet('Students may re-upload files until the due date (the latest upload replaces the previous).'),
  bullet('The teacher receives an in-app notification when a student uploads a file.'),
  bullet('The teacher downloads the file from the Assignment detail view in the Admin Portal to review.'),
  bullet('Files are stored in cloud object storage. Download links are available for 12 months after the due date.'),

  h3('9.1.3  Grading and Status'),
  dataTable(
    ['Status', 'Meaning'],
    [
      ['Pending',            'Assignment published; student has not been marked yet'],
      ['Submitted (Manual)', 'Teacher has manually marked the student as having submitted (no file)'],
      ['Submitted (File)',   'Student uploaded a file via the Student Portal'],
      ['Graded',             'A score has been entered by the teacher'],
      ['Missing',            'Due date passed; student has not submitted and is not marked Graded'],
    ],
    [2200, 7160]
  ),

  h2('9.2  Academic Results'),
  h3('9.2.1  Recording Results'),
  body('Admins or Teachers record examination results per student per subject. Each result contains: exam type (Monthly Test / Mid-Year / Final / Trial SPM), subject, score, maximum score, grade (auto-calculated), and exam date. Grade boundaries are configurable per branch.'),
  dataTable(
    ['Grade', 'Score Range', 'Description'],
    [
      ['A+', '90–100', 'Excellent'], ['A', '80–89', 'Excellent'], ['A-', '75–79', 'Very Good'],
      ['B+', '70–74', 'Good'], ['B', '60–69', 'Good'], ['C', '50–59', 'Average'],
      ['D', '40–49', 'Below Average'], ['E', '0–39', 'Fail'],
    ],
    [1200, 2400, 5760]
  ),

  h3('9.2.2  Performance Trend'),
  body('The system plots a student\'s scores over time per subject. The trend chart is visible on the Student Profile (Admin) and the Student Portal (Student/Parent).'),

  h2('9.3  Personal Performance Summary'),
  body('Each student\'s portal home screen shows: overall attendance rate, average academic score, assignments completed vs total, and improvement percentage vs the previous exam cycle.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 10 — Student Portal
// ═══════════════════════════════════════════════════════════════
const sec10 = [
  h1('10.  Student and Parent Portal'),
  h2('10.1  Overview'),
  body('The Student Portal is a mobile-first web application for students and parents. All data shown is read-only except for assignment file uploads and account settings. The portal is in English only. Dark mode is available via a toggle in the Profile screen.'),

  h2('10.2  Authentication'),
  decision('Single shared account', 'Students and parents share one portal account. The username is the parent\'s email; the password is shared.'),
  decision('Password only (v1.0)', 'Biometric authentication (Face ID / fingerprint) is deferred to Version 2.0.'),
  dataTable(
    ['Aspect', 'Detail'],
    [
      ['Username',        'Parent\'s registered email address'],
      ['Password',        'Admin-generated on registration; parent can change via profile settings'],
      ['Shared Access',   'Both the student and parent log in using the same email and password'],
      ['Password Reset',  'Reset link sent to the registered email; OTP via WhatsApp as fallback'],
      ['Biometric Login', 'Not available in v1.0; planned for v2.0 via Web Authentication API'],
      ['Session Timeout', '30 minutes of inactivity triggers automatic logout'],
      ['Account Creation','Admin-only; no self-registration'],
    ],
    [2600, 6760]
  ),

  h2('10.3  Portal Screens'),
  dataTable(
    ['Screen', 'Available To', 'Content'],
    [
      ['Home / Dashboard',  'Student & Parent', 'Greeting, stats (attendance %, pending tasks, fee status), today\'s classes, notice board'],
      ['My Schedule',       'Student & Parent', 'Day-picker calendar, timeline view with colour-coded classes, replacement alerts'],
      ['Attendance',        'Student & Parent', 'Monthly heatmap calendar, per-subject %, year-to-date summary'],
      ['Assignments',       'Student & Parent', 'Grouped Overdue / Upcoming / Submitted; file upload for enabled assignments; scores for graded items'],
      ['My Fees',           'Student & Parent', 'Current month status, full payment history, receipt download'],
      ['Profile / Settings','Student & Parent', 'Personal info, enrolled subjects, parent contact, password change, dark mode toggle'],
    ],
    [2200, 2000, 5160]
  ),

  h2('10.4  Dark Mode'),
  decision('Dark mode toggle', 'The Student Portal includes a dark mode toggle in the Profile screen. Preference is stored locally on the device.'),
  body('Dark mode applies a dark colour scheme across all Student Portal screens. The toggle is a simple on/off switch accessible from the Profile tab. The selected preference is persisted in browser localStorage so it survives page refreshes. The Admin Portal does not include dark mode in Version 1.0.'),

  h2('10.5  Language'),
  decision('English only', 'The Student Portal UI is in English only. A language toggle is not included in Version 1.0.'),
  body('All screen labels, buttons, error messages, and static text in the Student Portal are in English. Automated WhatsApp notifications sent to parents remain in Bahasa Malaysia by default, as parents may not be fluent in English.'),

  h2('10.6  Push Notifications'),
  body('Browser push notifications (where permission is granted) supplement WhatsApp for: new assignment published, assignment graded, class cancellation / rescheduling, and fee payment confirmed.'),

  h2('10.7  QR Check-In (Student Side)'),
  body('When a teacher generates a session QR code, students open the Student Portal, navigate to the active class on the Schedule screen, and tap "Check In". The portal activates the camera to scan the displayed QR. On success, the student sees a confirmation and the system records Present. Scanning outside the active window shows an error explaining the code has expired.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 11 — Non-Functional Requirements
// ═══════════════════════════════════════════════════════════════
const sec11 = [
  h1('11.  Non-Functional Requirements'),
  h2('11.1  Performance'),
  dataTable(
    ['Metric', 'Requirement'],
    [
      ['Page Load Time',        'All screens load within 2 seconds on a 4G mobile connection'],
      ['Attendance Submission', 'Submission of a 30-student roster completes within 3 seconds'],
      ['WhatsApp Delivery',     'Notification dispatched within 60 seconds of the triggering event'],
      ['Report Generation',     'All standard reports generate within 5 seconds'],
      ['File Upload',           'Assignment file upload (10 MB) completes within 15 seconds on a 4G connection'],
      ['Concurrent Users',      'System supports 50 concurrent Admin/Teacher users without degradation'],
    ],
    [3200, 6160]
  ),

  h2('11.2  Availability'),
  bullet('Target uptime: 99.5% monthly (excluding planned maintenance).'),
  bullet('Planned maintenance communicated at least 24 hours in advance.'),
  bullet('The Admin Portal remains fully functional if the WhatsApp API is temporarily unavailable; messages queue and send when the service recovers.'),

  h2('11.3  Security'),
  bullet('All data in transit encrypted via TLS 1.3.'),
  bullet('Passwords stored as bcrypt hashes (minimum cost factor 12).'),
  bullet('Role-based access control enforced on every API endpoint.'),
  bullet('Session tokens expire after 30 minutes of inactivity.'),
  bullet('Admin actions (payment recording, student status changes) are audit-logged with timestamp and user ID.'),
  bullet('Assignment file uploads are scanned for malware before being stored.'),
  bullet('Branch data isolation: a Branch Admin cannot access any data belonging to another branch, enforced at the database query level.'),
  bullet('Biometric authentication is deferred to Version 2.0.'),

  h2('11.4  Usability'),
  bullet('Admin Portal: desktop browsers (Chrome, Edge, Firefox, Safari — latest 2 versions).'),
  bullet('Student Portal: mobile browsers (iOS Safari 16+, Android Chrome 110+), minimum viewport 375 px.'),
  bullet('All critical workflows completable within 3 interactions.'),
  bullet('WCAG 2.1 Level AA compliance for both portals.'),
  bullet('Dark mode support in Student Portal (see Section 10.4).'),

  h2('11.5  Localisation'),
  decision('English only UI', 'The Admin Portal and Student Portal are in English only. A language toggle is not in scope for Version 1.0.'),
  bullet('UI language: English (Admin Portal and Student Portal).'),
  bullet('Automated WhatsApp messages: Bahasa Malaysia by default; Admin can edit templates.'),
  bullet('Date format: DD/MM/YYYY throughout.'),
  bullet('Currency: Malaysian Ringgit (RM), displayed to 2 decimal places.'),
  bullet('Timezone: Asia/Kuala_Lumpur (UTC+8).'),

  h2('11.6  Multi-Branch Scalability'),
  bullet('The system must support up to 20 branches under a single Platform account without performance degradation.'),
  bullet('Branch provisioning (adding a new branch) must be completable by a Platform Admin within 10 minutes.'),
  bullet('Cross-branch reports must complete within 10 seconds for up to 20 branches.'),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 12 — Decision Log (all resolved)
// ═══════════════════════════════════════════════════════════════
const sec12 = [
  h1('12.  Client Decision Log'),
  body('All ten questions raised in v1.0 have been answered by the client. The table below records each decision. Sections affected by each decision have been updated in-place throughout this document.'),
  spacer(80),
  dataTable(
    ['#', 'Question', 'Decision', 'Section(s) Updated'],
    [
      ['1',  'Student vs parent — shared or separate login?',                   'Single shared account (parent email + password; student logs in with same credentials)', '2.2, 3.2, 3.6, 10.2'],
      ['2',  'Online payment (FPX / DuitNow) in v1.0?',                        'Manual recording only. Online gateway deferred to Version 2.0',                          '2.5, 6.1, 6.4, 6.8'],
      ['3',  'Assignment file upload or tracking only?',                        'File upload required — PDF and image, max 10 MB, 3 files per submission',                 '9.1.1, 9.1.2, 11.1'],
      ['4',  'WhatsApp provider?',                                              'Official WhatsApp Business API (Meta)',                                                    '2.4, 7.1, 7.2'],
      ['5',  'QR self-check-in?',                                               'Included as an optional supplement to teacher-led marking',                               '1.3, 5.3, 10.7'],
      ['6',  'UI language — English, BM, or bilingual?',                        'English only. WhatsApp messages remain in BM by default',                                 '1.3, 2.5, 10.5, 11.5'],
      ['7',  'Single centre or multi-branch?',                                  'Multi-branch — multiple physical locations under one Platform account',                    '1.3, 2.2, 2.3, 3.1, 4.1, 6.2, 7.1, 8.1, 11.6'],
      ['8',  'Biometric authentication in Student Portal?',                     'Password only in Version 1.0. Biometric (Face ID / fingerprint) deferred to Version 2.0', '10.2, 11.3'],
      ['9',  '3-day advance WhatsApp fee reminder?',                            'Yes — automatic reminder sent 3 days before the branch-configured monthly due date',      '6.6, 7.2'],
      ['10', 'Dark mode in Student Portal?',                                    'Dark mode toggle included in Profile screen; Admin Portal dark mode deferred to v2.0',    '10.4, 11.4'],
    ],
    [300, 2600, 3800, 2660]
  ),
];

// ═══════════════════════════════════════════════════════════════
//  SECTION 13 — Appendix
// ═══════════════════════════════════════════════════════════════
const sec13 = [
  h1('13.  Appendix'),
  h2('A.  Module Summary'),
  dataTable(
    ['Module', 'Key Features (v1.0)', 'Primary Users'],
    [
      ['Student Management',     'Registration, shared account, profiles, status, multi-branch directory',         'Admin'],
      ['Class Management',       'Subject catalogue, timetable, rooms (per branch), holidays, replacements',       'Admin, Teacher'],
      ['Attendance',             'Teacher-led marking, optional QR self-check-in, absence WhatsApp, at-risk',      'Teacher, Admin, Student'],
      ['Fees & Billing',         'Manual payment recording, 3-day advance + monthly reminders, receipts',          'Admin'],
      ['Parent Communication',   'WhatsApp Business API, 11 automated templates, manual bulk messaging',           'Admin, System'],
      ['Reports',                '11 reports incl. cross-branch comparison (Platform Admin)',                       'Admin, Platform Admin'],
      ['Assignments & Results',  'Assignment creation, student file upload (PDF/image), grading, trend charts',    'Teacher, Admin, Student'],
      ['Student Portal',         'Shared login, English UI, dark mode, QR check-in, read + file upload',          'Student, Parent'],
    ],
    [2400, 4400, 2560]
  ),

  h2('B.  Version 2.0 Deferred Items'),
  body('The following features have been explicitly deferred from Version 1.0 and are candidates for Version 2.0:'),
  bullet('Online payment gateway integration (FPX / DuitNow QR auto-confirmation)'),
  bullet('Biometric authentication (Face ID / fingerprint) for the Student Portal'),
  bullet('Bahasa Malaysia / English bilingual UI toggle'),
  bullet('Dark mode for the Admin Portal'),
  bullet('Parent-to-teacher in-app messaging'),
  bullet('Automated SPM exam schedule integration'),

  h2('C.  Mockup Reference'),
  bodyRuns([{ text: 'Admin Portal: ', bold: true }, { text: 'mockup/index.html (9 screens — open in any browser)' }]),
  bodyRuns([{ text: 'Student Portal: ', bold: true }, { text: 'mockup/student-portal.html (7 screens incl. login)' }]),
  bodyRuns([{ text: 'GitHub Repository: ', bold: true }, { text: 'https://github.com/yeketou/skillup' }]),

  h2('D.  Revision History'),
  dataTable(
    ['Version', 'Date', 'Author', 'Changes'],
    [
      ['1.0', 'May 2026', 'SkillUp Product Team', 'Initial draft — mockup-based specification, 10 open questions raised'],
      ['1.1', 'May 2026', 'SkillUp Product Team', 'All 10 client decisions incorporated; 7 sections updated; multi-branch and QR check-in added; file upload, dark mode, and reminder schedule confirmed'],
    ],
    [1000, 1400, 2400, 4560]
  ),
];

// ═══════════════════════════════════════════════════════════════
//  BUILD DOCUMENT
// ═══════════════════════════════════════════════════════════════
const doc = new Document({
  styles: {
    default: { document: { run: { font: 'Arial', size: 22, color: '111827' } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 36, bold: true, font: 'Arial', color: C.navy },
        paragraph: { spacing: { before: 0, after: 200 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 28, bold: true, font: 'Arial', color: C.indigo },
        paragraph: { spacing: { before: 280, after: 120 }, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 24, bold: true, font: 'Arial', color: C.navy },
        paragraph: { spacing: { before: 200, after: 80 }, outlineLevel: 2 } },
    ]
  },
  numbering: {
    config: [
      { reference: 'bullets',
        levels: [
          { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
          { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 1080, hanging: 360 } } } },
        ]
      },
      { reference: 'numbers',
        levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        ]
      },
    ]
  },
  sections: [{
    properties: {
      page: {
        size:   { width: 12240, height: 15840 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    headers: {
      default: new Header({ children: [new Paragraph({
        border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: C.indigo, space: 2 } },
        children: [
          new TextRun({ text: 'SkillUp ', font: 'Arial', size: 18, bold: true, color: C.indigo }),
          new TextRun({ text: 'Tuition Management System — Functional Specification v1.1', font: 'Arial', size: 18, color: '6B7280' }),
        ]
      })] })
    },
    footers: {
      default: new Footer({ children: [new Paragraph({
        border: { top: { style: BorderStyle.SINGLE, size: 6, color: C.indigo, space: 2 } },
        alignment: AlignmentType.CENTER,
        children: [
          new TextRun({ text: 'Page ', font: 'Arial', size: 18, color: '6B7280' }),
          new TextRun({ children: [PageNumber.CURRENT], font: 'Arial', size: 18, color: '6B7280' }),
          new TextRun({ text: ' of ', font: 'Arial', size: 18, color: '6B7280' }),
          new TextRun({ children: [PageNumber.TOTAL_PAGES], font: 'Arial', size: 18, color: '6B7280' }),
          new TextRun({ text: '  |  v1.1  |  Confidential', font: 'Arial', size: 18, color: '6B7280' }),
        ]
      })] })
    },
    children: [
      ...coverPage,
      new Paragraph({ children: [new PageBreak()] }),
      new Paragraph({ spacing: { before: 0, after: 240 },
        children: [new TextRun({ text: 'Table of Contents', font: 'Arial', size: 36, bold: true, color: C.navy })] }),
      hr(),
      new TableOfContents('Table of Contents', {
        hyperlink: true, headingStyleRange: '1-3',
        stylesWithLevels: [
          { styleName: 'Heading 1', level: 1 },
          { styleName: 'Heading 2', level: 2 },
          { styleName: 'Heading 3', level: 3 },
        ]
      }),
      ...sec1, ...sec2, ...sec3, ...sec4, ...sec5,
      ...sec6, ...sec7, ...sec8, ...sec9, ...sec10,
      ...sec11, ...sec12, ...sec13,
    ]
  }]
});

Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync('C:/projects/skillup/SkillUp_Functional_Specification.docx', buf);
  console.log('Done — v1.1 written.');
}).catch(err => { console.error(err.message); process.exit(1); });
