import { http, type ApiRequestConfig } from "./client"

export type FeedbackType =
  | "ACCOUNT_LOGIN"
  | "EMAIL_SUBSCRIPTION"
  | "ERROR_BUG"
  | "FEATURE_REQUEST"
  | "BLOG_CONTENT"
  | "SERVICE_FEEDBACK"

export type FeedbackCreateCommand = {
  type: FeedbackType
  content: string
  email: string
}

export type FeedbackCreateResult = {
  feedbackId: number
}

export const feedbackApi = {
  create: async (
    command: FeedbackCreateCommand,
    config?: ApiRequestConfig,
  ): Promise<FeedbackCreateResult> => {
    const res = await http.post<FeedbackCreateResult>(
      "/api/feedback",
      command,
      config,
    )
    if (!res) throw new Error("EMPTY_FEEDBACK_RESPONSE")
    return res
  },
}
