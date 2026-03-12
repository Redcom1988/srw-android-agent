package com.redcom1988.domain.submission.interactor

import com.redcom1988.domain.submission.model.Submission
import com.redcom1988.domain.submission.repository.SubmissionRepository

class FinishPickup(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(id: Int, notes: String? = null): Submission {
        return submissionRepository.finishPickup(id, notes)
    }
}
