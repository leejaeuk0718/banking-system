"use client";
import { useState } from "react";
import { Bookmark, BookmarkCheck, Copy, Check } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import SourceChips from "./SourceChips";
import { formatFullDate } from "@/lib/utils";
import api from "@/lib/api";
import type { MessageResponse } from "@/types";

interface Props {
  message: MessageResponse;
  isBookmarked?: boolean;
  onBookmarkToggle?: (messageId: number, bookmarked: boolean) => void;
}

export default function MessageBubble({ message, isBookmarked = false, onBookmarkToggle }: Props) {
  const isUser = message.role === "USER";
  const [copied, setCopied] = useState(false);
  const [bookmarked, setBookmarked] = useState(isBookmarked);

  const handleCopy = () => {
    navigator.clipboard.writeText(message.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleBookmark = async () => {
    try {
      if (bookmarked) {
        // 북마크 삭제는 bookmarkId가 필요 — 단순화를 위해 toggle만 시각 처리
        setBookmarked(false);
        onBookmarkToggle?.(message.id, false);
      } else {
        await api.post("/bookmarks", { messageId: message.id });
        setBookmarked(true);
        onBookmarkToggle?.(message.id, true);
      }
    } catch {}
  };

  return (
    <div className={`flex gap-3 ${isUser ? "flex-row-reverse" : "flex-row"} group`}>
      {/* 아바타 */}
      <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 mt-0.5 ${
        isUser ? "bg-blue-600 text-white" : "bg-emerald-100 text-emerald-700"
      }`}>
        {isUser ? "나" : "AI"}
      </div>

      <div className={`max-w-[75%] ${isUser ? "items-end" : "items-start"} flex flex-col`}>
        <div className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
          isUser
            ? "bg-blue-600 text-white rounded-tr-sm"
            : "bg-white border border-gray-200 text-gray-800 rounded-tl-sm shadow-sm"
        }`}>
          {isUser ? (
            <p>{message.content}</p>
          ) : (
            <ReactMarkdown remarkPlugins={[remarkGfm]}
              components={{
                p: ({children}) => <p className="mb-2 last:mb-0">{children}</p>,
                ul: ({children}) => <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>,
                ol: ({children}) => <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>,
                code: ({children}) => <code className="bg-gray-100 text-gray-800 px-1.5 py-0.5 rounded text-xs font-mono">{children}</code>,
                strong: ({children}) => <strong className="font-semibold">{children}</strong>,
              }}
            >
              {message.content}
            </ReactMarkdown>
          )}
        </div>

        {/* 소스 칩 (AI 메시지만) */}
        {!isUser && message.sources && message.sources.length > 0 && (
          <div className="mt-1 px-1">
            <SourceChips sources={message.sources} />
          </div>
        )}

        {/* 액션 버튼 */}
        <div className={`flex items-center gap-1 mt-1 opacity-0 group-hover:opacity-100 transition-opacity ${isUser ? "flex-row-reverse" : "flex-row"}`}>
          <span className="text-xs text-gray-400">{formatFullDate(message.createdAt)}</span>
          <button onClick={handleCopy} className="p-1 rounded text-gray-400 hover:text-gray-600 transition-colors" title="복사">
            {copied ? <Check size={12} className="text-green-500" /> : <Copy size={12} />}
          </button>
          {!isUser && (
            <button onClick={handleBookmark} className="p-1 rounded text-gray-400 hover:text-yellow-500 transition-colors" title="북마크">
              {bookmarked ? <BookmarkCheck size={12} className="text-yellow-500" /> : <Bookmark size={12} />}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
