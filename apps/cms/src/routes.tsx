import { createBrowserRouter } from "react-router-dom";
import { Home } from "./pages/home";
import { NotFound } from "@/pages/not-found";
import { PublicLayout } from "./layouts/public-layout";
import { LoginPage } from "./pages/login";
import { RegisterPage } from "./pages/register";
import { ProtectedRoute } from "@/auth/protected-route";
import { Dashboard } from "@/pages/dashboard";

export const router = createBrowserRouter([
  {
    path: "/", element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> }
    ]
  },
  {
    element: <ProtectedRoute />,
    children: [{ path: "/dashboard", element: <Dashboard /> }]
  },
  {
    path: "*", element: <NotFound />
  }
])
