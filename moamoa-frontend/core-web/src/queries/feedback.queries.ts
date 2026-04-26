import { useMutation } from "@tanstack/react-query"
import {
  feedbackApi,
  type FeedbackCreateCommand,
  type FeedbackCreateResult,
} from "../api/feedback.api"

export const useCreateFeedbackMutation = () => {
  return useMutation<FeedbackCreateResult, Error, FeedbackCreateCommand>({
    mutationFn: (command) => feedbackApi.create(command),
  })
}
