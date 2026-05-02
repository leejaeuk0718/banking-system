"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import axios from "axios";
import api from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import Input from "@/components/ui/Input";
import Button from "@/components/ui/Button";
import type { UserResponse } from "@/types";

export default function LoginPage() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      // 로그인 → Authorization 헤더에서 토큰 추출
      const res = await axios.post("/api/users/login", form, { withCredentials: true });
      const token = res.headers["authorization"]?.replace("Bearer ", "") ?? "";
      // 내 정보 조회
      const { data: user } = await axios.get<UserResponse>("/api/users/me", {
        headers: { Authorization: `Bearer ${token}` },
        withCredentials: true,
      });
      setAuth(user, token);
      router.push("/chat");
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        setError("이메일 또는 비밀번호가 올바르지 않습니다.");
      } else {
        setError("로그인 중 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-md">
        {/* 로고 */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-blue-600 text-white text-2xl font-bold mb-3 shadow-lg">M</div>
          <h1 className="text-2xl font-bold text-gray-900">의료 AI 어시스턴트</h1>
          <p className="text-sm text-gray-500 mt-1">의료 문서 기반 질의응답 시스템</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-6">로그인</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="이메일"
              type="email"
              placeholder="example@email.com"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              autoFocus
            />
            <Input
              label="비밀번호"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">
                {error}
              </div>
            )}
            <Button type="submit" loading={loading} className="w-full" size="lg">
              로그인
            </Button>
          </form>
          <p className="text-center text-sm text-gray-500 mt-6">
            계정이 없으신가요?{" "}
            <Link href="/register" className="text-blue-600 hover:underline font-medium">
              회원가입
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
