import { createBrowserRouter } from "react-router-dom";
import { Home } from "./pages/home";
import { NotFound } from "@/pages/not-found";

export const router = createBrowserRouter([
  {
    path: "/", element: <Home />, index: true
  },
  {
    path: "*", element: <NotFound />
  }
])