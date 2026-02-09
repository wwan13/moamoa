import React from "react"
import ReactDOM from "react-dom/client"
import App from "./app/App"
import "./global.css"

import { BrowserRouter } from "react-router-dom"
import { AuthProvider } from "./auth/AuthContext"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"

// ✅ React Query client
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 30 * 1000, // 30초
        },
        mutations: {
            retry: 0,
        },
    },
})

ReactDOM.createRoot(document.getElementById("root")).render(
    <QueryClientProvider client={queryClient}>
        <BrowserRouter>
            <AuthProvider>
                <App />
            </AuthProvider>
        </BrowserRouter>
    </QueryClientProvider>
)