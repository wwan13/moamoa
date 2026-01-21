import {useMutation} from "@tanstack/react-query";
import {submissionApi} from "../api/submission.api.js";

export function useCreateSubmissionMutation() {
    return useMutation({
        mutationFn: submissionApi.create,
        onError: async () => {
        },
    })
}
