import React from "react"
import ReactDOM from "react-dom/client"
import App from "./app/App.jsx"
import "./global.css"

import { BrowserRouter } from "react-router-dom"
import { AuthProvider } from "./auth/AuthContext.jsx"

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
    <React.StrictMode>
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <AuthProvider>
                    <App />
                </AuthProvider>
            </BrowserRouter>
        </QueryClientProvider>
    </React.StrictMode>
)