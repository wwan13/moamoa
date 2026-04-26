import { useMutation } from "@tanstack/react-query"
import {
  submissionApi,
  type SubmissionCreateCommand,
  type SubmissionCreateResult,
} from "../api/submission.api"

export const useCreateSubmissionMutation = () => {
  return useMutation<SubmissionCreateResult, Error, SubmissionCreateCommand>({
    mutationFn: (command) => submissionApi.create(command),
  })
}
