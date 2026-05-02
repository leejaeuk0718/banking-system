"use client";
import { useEffect, useState, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { MessageSquare, Bookmark, FileText, Plus, Trash2, LogOut, User, ChevronRight } from "lucide-react";
import api from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import { formatDate } from "@/lib/utils";
import type { ConversationSummaryResponse } from "@/types";

export default function Sidebar() {
  const router = useRouter();
  const params = useParams();
  const { user, clearAuth, isAdmin } = useAuthStore();
  const [conversations, setConversations] = useState<ConversationSummaryResponse[]>([]);

  const fetchConversations = useCallback(async () => {
    try {
      const { data } = await api.get<ConversationSummaryResponse[]>("/conversations");
      setConversations(data);
    } catch {}
  }, []);

  useEffect(() => {
    fetchConversations();
  }, [fetchConversations]);

  const handleNewChat = () => router.push("/chat");

  const handleDelete = async (e: React.MouseEvent, id: number) => {
    e.preventDefault();
    e.stopPropagation();
    if (!confirm("이 대화를 삭제할까요?")) return;
    await api.delete(`/conversations/${id}`);
    setConversations((prev) => prev.filter((c) => c.id !== id));
    if (String(params?.id) === String(id)) router.push("/chat");
  };

  const handleLogout = async () => {
    try { await api.post("/users/logout"); } catch {}
    clearAuth();
    router.push("/login");
  };

  return (
    <aside className="flex flex-col w-64 h-screen bg-gray-900 text-gray-100 shrink-0">
      {/* 로고 */}
      <div className="flex items-center gap-2 px-4 py-4 border-b border-gray-700">
        <div className="w-8 h-8 rounded-lg bg-blue-500 flex items-center justify-center text-white font-bold text-sm">M</div>
        <span className="font-semibold text-sm">의료 AI 어시스턴트</span>
      </div>

      {/* 새 채팅 */}
      <div className="px-3 pt-3">
        <button
          onClick={handleNewChat}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-gray-300 hover:bg-gray-700 transition-colors border border-gray-600"
        >
          <Plus size={15} />
          새 대화
        </button>
      </div>

      {/* 대화 목록 */}
      <nav className="flex-1 overflow-y-auto px-3 py-2 space-y-0.5 mt-2">
        <p className="text-xs text-gray-500 px-2 py-1 font-medium uppercase tracking-wider">최근 대화</p>
        {conversations.length === 0 && (
          <p className="text-xs text-gray-600 px-2 py-2">대화가 없습니다</p>
        )}
        {conversations.map((conv) => (
          <Link
            key={conv.id}
            href={`/chat/${conv.id}`}
            className={`group flex items-center justify-between px-2 py-2 rounded-lg text-sm transition-colors ${
              String(params?.id) === String(conv.id)
                ? "bg-gray-700 text-white"
                : "text-gray-300 hover:bg-gray-800"
            }`}
          >
            <div className="flex items-center gap-2 min-w-0">
              <MessageSquare size={13} className="shrink-0 text-gray-400" />
              <div className="min-w-0">
                <p className="truncate text-xs font-medium">{conv.title}</p>
                <p className="text-xs text-gray-500">{formatDate(conv.lastMessageAt)}</p>
              </div>
            </div>
            <button
              onClick={(e) => handleDelete(e, conv.id)}
              className="opacity-0 group-hover:opacity-100 p-1 rounded hover:text-red-400 transition"
            >
              <Trash2 size={12} />
            </button>
          </Link>
        ))}
      </nav>

      {/* 하단 메뉴 */}
      <div className="border-t border-gray-700 px-3 py-3 space-y-0.5">
        <Link href="/bookmarks" className="flex items-center gap-2 px-2 py-2 rounded-lg text-sm text-gray-300 hover:bg-gray-800 transition-colors">
          <Bookmark size={14} /> 북마크
        </Link>
        {isAdmin() && (
          <Link href="/admin/documents" className="flex items-center gap-2 px-2 py-2 rounded-lg text-sm text-gray-300 hover:bg-gray-800 transition-colors">
            <FileText size={14} /> 문서 관리
          </Link>
        )}
        <div className="flex items-center justify-between px-2 py-2 text-xs text-gray-500">
          <div className="flex items-center gap-1.5">
            <User size={12} />
            <span className="truncate max-w-[110px]">{user?.name ?? user?.email}</span>
          </div>
          <button onClick={handleLogout} className="hover:text-gray-300 transition-colors" title="로그아웃">
            <LogOut size={13} />
          </button>
        </div>
      </div>
    </aside>
  );
}
