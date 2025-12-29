import React from "react"
import ReactDOM from "react-dom/client"
import App from "./app/App.jsx"
import "./global.css"
import {AuthProvider} from "./auth/AuthContext.jsx";
import { BrowserRouter } from "react-router-dom"

ReactDOM.createRoot(document.getElementById("root")).render(
    <BrowserRouter>
        <AuthProvider>
            <App/>
        </AuthProvider>
    </BrowserRouter>
)