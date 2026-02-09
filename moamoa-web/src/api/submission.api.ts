import { http, type ApiRequestConfig } from "./client"

export type SubmissionCreateCommand = {
  blogTitle: string
  blogUrl: string
  notificationEnabled: boolean
}

export type SubmissionCreateResult = {
  submissionId: number
}

export const submissionApi = {
  create: async (
    command: SubmissionCreateCommand,
    config?: ApiRequestConfig
  ): Promise<SubmissionCreateResult> => {
    const res = await http.post<SubmissionCreateResult>("/api/submission", command, config)
    if (!res) throw new Error("EMPTY_SUBMISSION_RESPONSE")
    return res
  },
}
