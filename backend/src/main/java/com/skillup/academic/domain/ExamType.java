package com.skillup.academic.domain;

public enum ExamType {
    SCHOOL_EXAM,    // student's actual school exam (e.g. Peperiksaan Pertengahan Tahun)
    INTERNAL_EXAM,  // test set by the tuition centre (linked to a class template)
    TRIAL_EXAM,     // trial / mock for PT3 or SPM — usually school-run but tracked here
    MOCK_EXAM       // tuition centre's own mock exam
}
