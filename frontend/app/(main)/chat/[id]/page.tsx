"use client";
import { useState, useRef, useEffect, useCallback } from "react";
import { useParams } from "next/navigation";
import { Send, Bot } from "lucide-react";
import api from "@/lib/api";
import MessageBubble from "@/components/chat/MessageBubble";
import type { ChatResponse, ConversationDetailResponse, MessageResponse } from "@/types";

export default function ConversationPage() {
  const { id } = useParams<{ id: string }>();
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const fetchConversation = useCallback(async () => {
    try {
      const { data } = await api.get<ConversationDetailResponse>(`/conversations/${id}`);
      setTitle(data.title);
      setMessages(data.messages);
    } catch {} finally {
      setInitialLoading(false);
    }
  }, [id]);

  useEffect(() => {
    setInitialLoading(true);
    fetchConversation();
  }, [fetchConversation]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return;
    setInput("");
    setLoading(true);

    const userMsg: MessageResponse = {
      id: Date.now(),
      role: "USER",
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);

    try {
      const { data } = await api.post<ChatResponse>("/chat", {
        conversationId: Number(id),
        message: text,
      });
      const aiMsg: MessageResponse = {
        id: data.messageId,
        role: "ASSISTANT",
        content: data.content,
        createdAt: data.createdAt,
        sources: data.sources,
      };
      setMessages((prev) => [...prev, aiMsg]);
    } catch {
      setMessages((prev) => [...prev, {
        id: Date.now(), role: "ASSISTANT",
        content: "⚠️ 응답을 받지 못했습니다. 다시 시도해주세요.",
        createdAt: new Date().toISOString(),
      }]);
    } finally {
      setLoading(false);
      textareaRef.current?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  if (initialLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <div className="animate-spin w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-gray-50">
      {/* 헤더 */}
      <div className="flex items-center gap-3 px-6 py-4 bg-white border-b border-gray-200">
        <Bot size={20} className="text-blue-600" />
        <h1 className="font-semibold text-gray-800 truncate">{title || "대화"}</h1>
      </div>

      {/* 메시지 */}
      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
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

      {/* 입력 */}
      <div className="px-4 py-4 bg-white border-t border-gray-200">
        <div className="flex items-end gap-2 bg-gray-50 border border-gray-300 rounded-2xl px-4 py-3 focus-within:border-blue-400 focus-within:ring-1 focus-within:ring-blue-400 transition">
          <textarea ref={textareaRef} rows={1} value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="의료 관련 질문을 입력하세요 (Shift+Enter: 줄바꿈)"
            className="flex-1 resize-none bg-transparent text-sm outline-none text-gray-800 placeholder-gray-400 max-h-32"
            style={{ lineHeight: "1.5" }}
          />
          <button onClick={handleSend} disabled={!input.trim() || loading}
            className="w-8 h-8 rounded-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-200 flex items-center justify-center transition-colors shrink-0">
            <Send size={14} className={input.trim() ? "text-white" : "text-gray-400"} />
          </button>
        </div>
        <p className="text-xs text-gray-400 text-center mt-2">AI 답변은 참고용이며 전문의 진료를 대체하지 않습니다</p>
      </div>
    </div>
  );
}
