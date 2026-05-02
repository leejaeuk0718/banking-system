"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import axios from "axios";
import Input from "@/components/ui/Input";
import Button from "@/components/ui/Button";

// 백엔드 패턴: 영문+숫자+특수문자(@#$%^&+=!) 포함, 8~16자, 공백 불가
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@#$%^&+=!])(?!.*\s).{8,16}$/;
// 백엔드 패턴: 01로 시작, 8~9자리 숫자 (하이픈 없이)
const PHONE_REGEX = /^01[0-9]{8,9}$/;
// 백엔드 패턴: YYYYMMDD 8자리 숫자
const BIRTHDATE_REGEX = /^\d{8}$/;

export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    birthDate: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [globalError, setGlobalError] = useState("");

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!form.name.trim()) newErrors.name = "이름을 입력해주세요";
    if (!form.email.includes("@")) newErrors.email = "올바른 이메일 형식이 아닙니다";
    if (!PASSWORD_REGEX.test(form.password))
      newErrors.password = "영문, 숫자, 특수문자(@#$%^&+=!) 모두 포함 8~16자";
    if (form.password !== form.confirmPassword)
      newErrors.confirmPassword = "비밀번호가 일치하지 않습니다";
    if (!PHONE_REGEX.test(form.phone))
      newErrors.phone = "하이픈 없이 입력하세요 (예: 01012345678)";
    if (!BIRTHDATE_REGEX.test(form.birthDate))
      newErrors.birthDate = "YYYYMMDD 형식으로 입력하세요 (예: 19950101)";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    setGlobalError("");
    setLoading(true);
    try {
      await axios.post("/api/users/register", {
        name: form.name,
        email: form.email,
        password: form.password,
        phone: form.phone,
        birthDate: form.birthDate,
      });
      router.push("/login?registered=1");
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const data = err.response?.data;
        if (Array.isArray(data)) setGlobalError(data.join(", "));
        else if (typeof data === "string") setGlobalError(data);
        else setGlobalError("회원가입 중 오류가 발생했습니다.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4 py-8">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-blue-600 text-white text-2xl font-bold mb-3 shadow-lg">M</div>
          <h1 className="text-2xl font-bold text-gray-900">의료 AI 어시스턴트</h1>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-6">회원가입</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="이름" placeholder="홍길동" value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              error={errors.name} autoFocus />

            <Input label="이메일" type="email" placeholder="example@email.com"
              value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })}
              error={errors.email} />

            <div>
              <Input label="비밀번호" type="password" placeholder="영문+숫자+특수문자, 8~16자"
                value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })}
                error={errors.password} />
              {!errors.password && (
                <p className="mt-1 text-xs text-gray-400">영문, 숫자, 특수문자(@#$%^&+=!) 모두 포함 8~16자</p>
              )}
            </div>

            <Input label="비밀번호 확인" type="password" placeholder="비밀번호를 다시 입력하세요"
              value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              error={errors.confirmPassword} />

            <Input label="전화번호" type="tel" placeholder="01012345678 (하이픈 없이)"
              value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })}
              error={errors.phone} />

            <Input label="생년월일" placeholder="19950101 (YYYYMMDD)"
              value={form.birthDate} onChange={(e) => setForm({ ...form, birthDate: e.target.value })}
              error={errors.birthDate} maxLength={8} />

            {globalError && (
              <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-600">{globalError}</div>
            )}
            <Button type="submit" loading={loading} className="w-full" size="lg">
              회원가입
            </Button>
          </form>
          <p className="text-center text-sm text-gray-500 mt-6">
            이미 계정이 있으신가요?{" "}
            <Link href="/login" className="text-blue-600 hover:underline font-medium">로그인</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
