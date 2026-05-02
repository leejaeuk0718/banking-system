"use client";
import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Send, Bot } from "lucide-react";
import api from "@/lib/api";
import MessageBubble from "@/components/chat/MessageBubble";
import type { ChatResponse, MessageResponse } from "@/types";

export default function ChatPage() {
  const router = useRouter();
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [convId, setConvId] = useState<number | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;
    setInput("");
    setLoading(true);

    // 사용자 메시지 즉시 표시
    const userMsg: MessageResponse = {
      id: Date.now(),
      role: "USER",
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);

    try {
      const { data } = await api.post<ChatResponse>("/chat", {
        conversationId: convId,
        message: text,
      });

      if (!convId) {
        setConvId(data.conversationId);
        router.replace(`/chat/${data.conversationId}`);
      }

      const aiMsg: MessageResponse = {
        id: data.messageId,
        role: "ASSISTANT",
        content: data.content,
        createdAt: data.createdAt,
        sources: data.sources,
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now(),
          role: "ASSISTANT",
          content: "⚠️ 응답을 받지 못했습니다. 다시 시도해주세요.",
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setLoading(false);
      textareaRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="flex flex-col h-full bg-gray-50">
      {/* 헤더 */}
      <div className="flex items-center gap-3 px-6 py-4 bg-white border-b border-gray-200">
        <Bot size={20} className="text-blue-600" />
        <h1 className="font-semibold text-gray-800">의료 AI 어시스턴트</h1>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-16 h-16 rounded-2xl bg-blue-100 flex items-center justify-center mb-4">
              <Bot size={32} className="text-blue-500" />
            </div>
            <h2 className="text-xl font-semibold text-gray-700 mb-2">무엇이든 질문하세요</h2>
            <p className="text-gray-400 text-sm max-w-sm">
              업로드된 의료 문서를 기반으로 정확한 답변을 제공합니다
            </p>
            <div className="grid grid-cols-1 gap-2 mt-6 w-full max-w-md">
              {["고혈압 진단 기준은 무엇인가요?", "당뇨병 1형과 2형의 차이는?", "항생제 내성이란 무엇인가요?"].map((q) => (
                <button key={q} onClick={() => setInput(q)}
                  className="text-left px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm text-gray-600 hover:border-blue-300 hover:text-blue-600 transition-colors">
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} />
        ))}
        {loading && (
          <div className="flex gap-3">
            <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-xs font-bold text-emerald-700 shrink-0">AI</div>
            <div className="bg-white border border-gray-200 rounded-2xl rounded-tl-sm px-4 py-3 shadow-sm">
              <div className="flex gap-1">
                {[0, 1, 2].map((i) => (
                  <span key={i} className="w-2 h-2 rounded-full bg-gray-300 animate-bounce"
                    style={{ animationDelay: `${i * 0.15}s` }} />
                ))}
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* 입력 영역 */}
      <div className="px-4 py-4 bg-white border-t border-gray-200">
        <div className="flex items-end gap-2 bg-gray-50 border border-gray-300 rounded-2xl px-4 py-3 focus-within:border-blue-400 focus-within:ring-1 focus-within:ring-blue-400 transition">
          <textarea
            ref={textareaRef}
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="의료 관련 질문을 입력하세요 (Shift+Enter: 줄바꿈)"
            className="flex-1 resize-none bg-transparent text-sm outline-none text-gray-800 placeholder-gray-400 max-h-32"
            style={{ lineHeight: "1.5" }}
          />
          <button
            onClick={handleSend}
            disabled={!input.trim() || loading}
            className="w-8 h-8 rounded-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-200 flex items-center justify-center transition-colors shrink-0"
          >
            <Send size={14} className={input.trim() ? "text-white" : "text-gray-400"} />
          </button>
        </div>
        <p className="text-xs text-gray-400 text-center mt-2">AI 답변은 참고용이며 전문의 진료를 대체하지 않습니다</p>
      </div>
    </div>
  );
}
