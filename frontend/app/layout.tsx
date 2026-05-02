import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "의료 RAG | Medical AI Assistant",
  description: "의료 문서 기반 AI 질의응답 시스템",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
