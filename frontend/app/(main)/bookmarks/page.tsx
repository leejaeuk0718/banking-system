"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Bookmark, Trash2, MessageSquare, Calendar } from "lucide-react";
import api from "@/lib/api";
import { formatFullDate } from "@/lib/utils";
import type { BookmarkResponse } from "@/types";

export default function BookmarksPage() {
  const [bookmarks, setBookmarks] = useState<BookmarkResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<BookmarkResponse[]>("/bookmarks")
      .then(({ data }) => setBookmarks(data))
      .finally(() => setLoading(false));
  }, []);

  const handleDelete = async (id: number) => {
    if (!confirm("북마크를 삭제할까요?")) return;
    await api.delete(`/bookmarks/${id}`);
    setBookmarks((prev) => prev.filter((b) => b.id !== id));
  };

  return (
    <div className="h-full overflow-y-auto bg-gray-50">
      <div className="max-w-3xl mx-auto px-6 py-8">
        {/* 헤더 */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-xl bg-yellow-100 flex items-center justify-center">
            <Bookmark size={20} className="text-yellow-600" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">북마크</h1>
            <p className="text-sm text-gray-500">저장한 AI 답변 {bookmarks.length}개</p>
          </div>
        </div>

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full" />
          </div>
        ) : bookmarks.length === 0 ? (
          <div className="text-center py-20">
            <div className="w-16 h-16 rounded-2xl bg-gray-100 flex items-center justify-center mx-auto mb-4">
              <Bookmark size={28} className="text-gray-400" />
            </div>
            <p className="text-gray-500 font-medium">저장된 북마크가 없습니다</p>
            <p className="text-gray-400 text-sm mt-1">채팅에서 AI 답변을 북마크로 저장해보세요</p>
            <Link href="/chat" className="inline-block mt-4 px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors">
              채팅 시작하기
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {bookmarks.map((bm) => (
              <div key={bm.id} className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow group">
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-2 text-sm text-gray-500">
                    <MessageSquare size={13} />
                    <span className="font-medium text-gray-700 truncate max-w-xs">{bm.conversationTitle}</span>
                  </div>
                  <button
                    onClick={() => handleDelete(bm.id)}
                    className="opacity-0 group-hover:opacity-100 p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>

                <p className="text-gray-800 text-sm leading-relaxed line-clamp-4">{bm.content}</p>

                <div className="flex items-center gap-1.5 mt-3 text-xs text-gray-400">
                  <Calendar size={11} />
                  <span>{formatFullDate(bm.createdAt)}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
