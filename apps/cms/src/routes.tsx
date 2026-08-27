import { createBrowserRouter } from "react-router-dom";
import { Home } from "./pages/home";
import { NotFound } from "@/pages/not-found";
import { PublicLayout } from "./layouts/public-layout";
import { LoginPage } from "./pages/login";

export const router = createBrowserRouter([
  {
    path: "/", element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: "login", element: <LoginPage /> }
    ]
  },
  {
    path: "*", element: <NotFound />
  }
])