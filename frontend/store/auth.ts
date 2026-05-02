import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { UserResponse } from "@/types";

interface AuthState {
  user: UserResponse | null;
  accessToken: string | null;
  setAuth: (user: UserResponse, token: string) => void;
  clearAuth: () => void;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      setAuth: (user, token) => {
        localStorage.setItem("accessToken", token);
        set({ user, accessToken: token });
      },
      clearAuth: () => {
        localStorage.removeItem("accessToken");
        set({ user: null, accessToken: null });
      },
      isAdmin: () => get().user?.role === "ADMIN",
    }),
    {
      name: "auth-storage",
      partialize: (state) => ({ user: state.user, accessToken: state.accessToken }),
    }
  )
);
