/**
 * SkillUp API Client
 * Single source of truth for all backend calls.
 * Handles JWT auth, token refresh, and 401 redirects.
 */
const API = (() => {
    const BASE = '/api';

    // ── Token management ─────────────────────────────────────────────────────
    const getToken    = () => localStorage.getItem('skillup_token');
    const setToken    = t  => localStorage.setItem('skillup_token', t);
    const getRefresh  = () => localStorage.getItem('skillup_refresh');
    const setRefresh  = t  => localStorage.setItem('skillup_refresh', t);
    const clearAuth   = () => {
        localStorage.removeItem('skillup_token');
        localStorage.removeItem('skillup_refresh');
        localStorage.removeItem('skillup_user');
    };

    // ── Core request ──────────────────────────────────────────────────────────
    async function request(method, path, body) {
        const headers = { 'Content-Type': 'application/json' };
        const token = getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const res = await fetch(`${BASE}${path}`, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });

        if (res.status === 401) {
            clearAuth();
            window.location.href = '/login.html';
            return null;
        }
        if (res.status === 204) return null;

        const data = await res.json().catch(() => null);
        if (!res.ok) {
            const msg = data?.detail || data?.message || `Request failed (${res.status})`;
            throw new Error(msg);
        }
        return data;
    }

    const get   = (path)        => request('GET',    path);
    const post  = (path, body)  => request('POST',   path, body);
    const put   = (path, body)  => request('PUT',    path, body);
    const patch = (path, body)  => request('PATCH',  path, body);
    const del   = (path)        => request('DELETE', path);

    // ── Auth ──────────────────────────────────────────────────────────────────
    async function login(email, password) {
        const res = await post('/portal/auth/login', { email, password });
        if (res?.access_token) {
            setToken(res.access_token);
            setRefresh(res.refresh_token);
        }
        return res;
    }

    function logout() {
        clearAuth();
        window.location.href = '/login.html';
    }

    function isLoggedIn() {
        return !!getToken();
    }

    // ── Dashboard / Reports ───────────────────────────────────────────────────
    const getDashboard = () => get('/reports/dashboard');

    // ── Branches ──────────────────────────────────────────────────────────────
    const getBranches      = ()        => get('/branches');
    const createBranch     = data      => post('/branches', data);
    const updateBranch     = (id,data) => put(`/branches/${id}`, data);
    const deactivateBranch = id        => patch(`/branches/${id}/deactivate`);
    const deleteBranch     = id        => request('DELETE', `/branches/${id}`);
    const getSubjects      = (activeOnly = true) => get(`/subjects?activeOnly=${activeOnly}`);
    const createSubject    = data      => post('/subjects', data);
    const updateSubject    = (id,data) => put(`/subjects/${id}`, data);
    const deleteSubject    = id        => request('DELETE', `/subjects/${id}`);

    // ── Students ──────────────────────────────────────────────────────────────
    const getStudents = (params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/students${q ? '?' + q : ''}`);
    };
    const getStudent      = id        => get(`/students/${id}`);
    const updateStudent   = (id,data) => put(`/students/${id}`, data);
    const changeStatus    = (id,data) => patch(`/students/${id}/status`, data);
    const addParent       = (id,data) => post(`/students/${id}/parents`, data);
    const activateStudentPortal = id  => post(`/students/${id}/portal/activate`, {});
    const deleteStudent   = id        => request('DELETE', `/students/${id}`);
    const createStudent   = data => post('/students', data);
    const getStudentFees  = id   => get(`/students/${id}/fees`);
    const getStudentReport   = id => get(`/reports/students/${id}`);
    const getStudentTimetable = id => get(`/students/${id}/timetable`);

    // ── Classes & Sessions ────────────────────────────────────────────────────
    const getTemplates   = (params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/classes${q ? '?' + q : ''}`);       // GET /classes (no /templates suffix)
    };
    const getTemplate    = id        => get(`/classes/${id}`);
    const createTemplate = data      => post('/classes', data);
    const updateTemplate = (id,data) => put(`/classes/${id}`, data);
    const getEnrollments = classId => get(`/classes/${classId}/enrollments`);
    const enrollStudent  = (classId, data) => post(`/classes/${classId}/enroll`, data);
    const dropStudent    = (classId, studentId) => request('DELETE', `/classes/${classId}/enrollments/${studentId}`);

    const getSessions = (templateId, from, to) => {
        const q = new URLSearchParams({ ...(from && {from}), ...(to && {to}) }).toString();
        return get(`/classes/${templateId}/sessions${q ? '?' + q : ''}`);
    };
    const getUpcomingSessions = (from) => {
        const today = from || new Date().toISOString().slice(0,10);
        return get(`/sessions/upcoming?from=${today}`);
    };
    const getSessionAttendance  = sid   => get(`/sessions/${sid}/attendance`);
    const markAttendance        = (sid, records) => post(`/sessions/${sid}/attendance`, { records });
    const updateAttendanceMark  = (sid, studentId, status, notes) =>
        patch(`/sessions/${sid}/students/${studentId}/attendance`, { status, notes });

    const generateSessions = (templateId, fromDate, toDate) =>
        post(`/classes/${templateId}/sessions/generate`, { fromDate, toDate });

    // ── Fees ──────────────────────────────────────────────────────────────────
    const getFees = (params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/fees${q ? '?' + q : ''}`);
    };
    const recordPayment   = (id, data) => post(`/fees/${id}/pay`, data);
    const waiveFee        = (id, data) => patch(`/fees/${id}/waive`, data);
    const generateFees    = data => post('/fees/generate', data);

    // ── Assignments ───────────────────────────────────────────────────────────
    const getAssignments = templateId =>
        get(`/classes/${templateId}/assignments`);
    const createAssignment   = data  => post('/assignments', data);
    const publishAssignment  = id    => post(`/assignments/${id}/publish`, {});
    const closeAssignment    = id    => post(`/assignments/${id}/close`, {});
    const getSubmissions     = id    => get(`/assignments/${id}/submissions`);
    const gradeSubmission    = (subId, data) => patch(`/submissions/${subId}/grade`, data);
    const excuseSubmission   = (subId, data) => patch(`/submissions/${subId}/excuse`, data);

    // ── Exams / Results ───────────────────────────────────────────────────────
    const getClassResults  = (templateId, examName) => {
        const q = examName ? `?examName=${encodeURIComponent(examName)}` : '';
        return get(`/classes/${templateId}/exam-results${q}`);
    };
    const getStudentResults = (studentId, params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/students/${studentId}/exam-results${q ? '?' + q : ''}`);
    };
    const recordResult      = data      => post('/exam-results', data);
    const bulkRecordResult  = data      => post('/exam-results/bulk', data);
    const updateExamResult  = (id,data) => patch(`/exam-results/${id}`, data);

    // ── Staff ─────────────────────────────────────────────────────────────────
    const getStaff = (params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/staff${q ? '?' + q : ''}`);
    };
    const createStaff      = data => post('/staff', data);
    const activatePortal   = id   => post(`/staff/${id}/activate-portal`, {});

    // ── Communications ────────────────────────────────────────────────────────
    const sendMessage  = data  => post('/communications/send', data);
    const getCommLogs  = (params = {}) => {
        const q = new URLSearchParams(params).toString();
        return get(`/communications/logs${q ? '?' + q : ''}`);
    };

    // ── Admin ─────────────────────────────────────────────────────────────────
    const generateSessions2 = (weeks) =>
        post(`/admin/sessions/generate-upcoming${weeks ? '?lookaheadWeeks='+weeks : ''}`);
    const getHolidays    = year => get(`/admin/public-holidays?year=${year || new Date().getFullYear()}`);
    const addHoliday     = data => post('/admin/public-holidays', data);
    const deleteHoliday  = id   => request('DELETE', `/admin/public-holidays/${id}`);
    const getFinancialReport  = (from,to,branchId) => {
        const q = new URLSearchParams({ fromMonth:from, toMonth:to, ...(branchId?{branchId}:{}) }).toString();
        return get(`/reports/financial?${q}`);
    };
    const getAttendanceReport = (from,to,branchId,threshold=80) => {
        const q = new URLSearchParams({ fromDate:from, toDate:to, lowThreshold:threshold, ...(branchId?{branchId}:{}) }).toString();
        return get(`/reports/attendance?${q}`);
    };
    const getAcademicReport   = (from,to,branchId) => {
        const q = new URLSearchParams({ fromDate:from, toDate:to, ...(branchId?{branchId}:{}) }).toString();
        return get(`/reports/academic?${q}`);
    };

    return {
        // Token
        getToken, setToken, clearAuth, isLoggedIn,
        // Auth
        login, logout,
        // Data
        getDashboard,
        getBranches, createBranch, updateBranch, deactivateBranch, deleteBranch,
        getSubjects, createSubject, updateSubject, deleteSubject,
        getStudents, getStudent, updateStudent, changeStatus, addParent,
        activateStudentPortal, deleteStudent,
        createStudent, getStudentFees, getStudentReport,
        getTemplates, getTemplate, createTemplate, updateTemplate,
        getEnrollments, enrollStudent, dropStudent,
        getSessions, getUpcomingSessions, getSessionAttendance, markAttendance, updateAttendanceMark, generateSessions,
        updateSessionStatus: (id, status, notes) => patch(`/sessions/${id}/status`, { status, notes: notes||null }),
        getFees, recordPayment, waiveFee, generateFees,
        getAssignments, createAssignment, publishAssignment, closeAssignment,
        getSubmissions, gradeSubmission, excuseSubmission,
        getClassResults, getStudentResults, recordResult, bulkRecordResult, updateExamResult,
        getStudentTimetable,
        getStaff, createStaff, activatePortal,
        sendMessage, getCommLogs,
        generateSessions2, getHolidays, addHoliday, deleteHoliday,
        getFinancialReport, getAttendanceReport, getAcademicReport,
    };
})();
