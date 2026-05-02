"use client";
import { useEffect, useState, useRef } from "react";
import { Upload, Trash2, FileText, CheckCircle, XCircle, Clock, ChevronDown, ChevronUp } from "lucide-react";
import api from "@/lib/api";
import { useAuthStore } from "@/store/auth";
import { formatFullDate } from "@/lib/utils";
import Button from "@/components/ui/Button";
import type { DocumentResponse, DocumentDetailResponse, DocumentStatus } from "@/types";

const STATUS_MAP: Record<DocumentStatus, { label: string; icon: React.ReactNode; color: string }> = {
  PROCESSING: { label: "처리 중",  icon: <Clock size={13} />,        color: "text-yellow-600 bg-yellow-50 border-yellow-200" },
  READY:      { label: "적재 완료", icon: <CheckCircle size={13} />, color: "text-green-600 bg-green-50 border-green-200" },
  FAILED:     { label: "실패",     icon: <XCircle size={13} />,      color: "text-red-600 bg-red-50 border-red-200" },
};

function StatusBadge({ status }: { status: DocumentStatus }) {
  const { label, icon, color } = STATUS_MAP[status];
  return (
    <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded-full border ${color}`}>
      {icon}{label}
    </span>
  );
}

export default function AdminDocumentsPage() {
  const { isAdmin } = useAuthStore();
  const [docs, setDocs] = useState<DocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState<Record<number, { step: string; percent: number }>>({});
  const [expanded, setExpanded] = useState<number | null>(null);
  const [detail, setDetail] = useState<DocumentDetailResponse | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [source, setSource] = useState("");
  const fileRef = useRef<HTMLInputElement>(null);

  const fetchDocs = async () => {
    try {
      const { data } = await api.get<DocumentResponse[]>("/admin/documents");
      setDocs(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDocs(); }, []);

  // SSE 진행률 구독
  const subscribeProgress = (docId: number) => {
    const token = localStorage.getItem("accessToken");
    const url = `/api/admin/documents/${docId}/progress`;
    const es = new EventSource(url + `?token=${token}`);

    es.addEventListener("progress", (e) => {
      const data = JSON.parse(e.data);
      setProgress((prev) => ({ ...prev, [docId]: data }));
    });
    es.addEventListener("done", () => {
      setProgress((prev) => ({ ...prev, [docId]: { step: "완료", percent: 100 } }));
      fetchDocs();
      es.close();
    });
    es.addEventListener("error", (e) => {
      try {
        const data = JSON.parse((e as MessageEvent).data);
        setProgress((prev) => ({ ...prev, [docId]: { step: data.step, percent: -1 } }));
      } catch {}
      es.close();
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) { alert("파일을 선택해주세요"); return; }
    if (!source.trim()) { alert("출처명을 입력해주세요"); return; }

    setUploading(true);
    const formData = new FormData();
    formData.append("file", selectedFile);
    formData.append("source", source.trim());

    try {
      const { data } = await api.post<DocumentResponse>("/admin/documents", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setDocs((prev) => [data, ...prev]);
      subscribeProgress(data.id);
      setSelectedFile(null);
      setSource("");
      if (fileRef.current) fileRef.current.value = "";
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      alert(msg ?? "업로드 실패. 백엔드 로그를 확인해주세요.");
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("이 문서를 삭제할까요? 벡터 임베딩도 함께 삭제됩니다.")) return;
    await api.delete(`/admin/documents/${id}`);
    setDocs((prev) => prev.filter((d) => d.id !== id));
    if (expanded === id) setExpanded(null);
  };

  const handleExpand = async (id: number) => {
    if (expanded === id) { setExpanded(null); setDetail(null); return; }
    setExpanded(id);
    const { data } = await api.get<DocumentDetailResponse>(`/admin/documents/${id}`);
    setDetail(data);
  };

  if (!isAdmin()) {
    return (
      <div className="flex h-full items-center justify-center text-gray-500">
        <div className="text-center">
          <XCircle size={40} className="mx-auto mb-3 text-red-400" />
          <p className="font-medium">접근 권한이 없습니다</p>
          <p className="text-sm mt-1">관리자 계정으로 로그인해주세요</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto bg-gray-50">
      <div className="max-w-4xl mx-auto px-6 py-8">
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center">
            <FileText size={20} className="text-blue-600" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">문서 관리</h1>
            <p className="text-sm text-gray-500">의료 RAG 지식베이스 관리</p>
          </div>
        </div>

        {/* 업로드 카드 */}
        <div className="bg-white rounded-xl border border-gray-200 p-6 mb-6 shadow-sm">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">📄 새 문서 업로드</h2>
          <div className="flex flex-col gap-3">
            <input
              type="text"
              value={source}
              onChange={(e) => setSource(e.target.value)}
              placeholder="출처명 (예: 2024 대한고혈압학회 진료지침)"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="flex gap-3">
              <label className={`flex-1 flex items-center gap-2 px-4 py-2 border-2 border-dashed rounded-lg text-sm cursor-pointer transition-colors ${
                selectedFile
                  ? "border-blue-400 bg-blue-50 text-blue-700"
                  : "border-gray-300 text-gray-500 hover:border-blue-400 hover:text-blue-600"
              }`}>
                <Upload size={14} className="shrink-0" />
                <span className="truncate">
                  {selectedFile ? selectedFile.name : "파일 선택 (PDF, TXT, DOCX)"}
                </span>
                <input
                  ref={fileRef}
                  type="file"
                  accept=".pdf,.txt,.docx,.doc"
                  className="hidden"
                  onChange={handleFileChange}
                />
              </label>
              <Button
                onClick={handleUpload}
                loading={uploading}
                disabled={!selectedFile || !source.trim()}
                size="md"
              >
                업로드
              </Button>
            </div>
          </div>
          <p className="text-xs text-gray-400 mt-2">지원 형식: PDF, TXT, DOCX · 최대 50MB</p>
        </div>

        {/* 문서 목록 */}
        {loading ? (
          <div className="flex justify-center py-20">
            <div className="animate-spin w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full" />
          </div>
        ) : docs.length === 0 ? (
          <div className="text-center py-20 text-gray-500">
            <FileText size={32} className="mx-auto mb-3 text-gray-300" />
            <p>업로드된 문서가 없습니다</p>
          </div>
        ) : (
          <div className="space-y-3">
            {docs.map((doc) => (
              <div key={doc.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
                <div className="flex items-center gap-4 px-5 py-4">
                  <div className="w-9 h-9 rounded-lg bg-blue-50 flex items-center justify-center shrink-0">
                    <FileText size={16} className="text-blue-500" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm text-gray-800 truncate">{doc.source}</p>
                    <p className="text-xs text-gray-400 mt-0.5">{doc.originalFilename} · {formatFullDate(doc.createdAt)} · 청크 {doc.chunkCount}개</p>
                    {doc.errorMessage && (
                      <p className="text-xs text-red-500 mt-0.5">오류: {doc.errorMessage}</p>
                    )}
                    {/* 진행 바 */}
                    {progress[doc.id] && progress[doc.id].percent >= 0 && progress[doc.id].percent < 100 && (
                      <div className="mt-2">
                        <div className="flex justify-between text-xs text-gray-400 mb-1">
                          <span>{progress[doc.id].step}</span>
                          <span>{progress[doc.id].percent}%</span>
                        </div>
                        <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                          <div className="h-full bg-blue-500 rounded-full transition-all"
                            style={{ width: `${progress[doc.id].percent}%` }} />
                        </div>
                      </div>
                    )}
                  </div>
                  <StatusBadge status={doc.status} />
                  <div className="flex items-center gap-1 shrink-0">
                    <button onClick={() => handleExpand(doc.id)}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-blue-500 hover:bg-blue-50 transition">
                      {expanded === doc.id ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
                    </button>
                    <button onClick={() => handleDelete(doc.id)}
                      className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>

                {/* 청크 상세 */}
                {expanded === doc.id && detail && detail.id === doc.id && (
                  <div className="border-t border-gray-100 bg-gray-50 px-5 py-4 max-h-72 overflow-y-auto">
                    <p className="text-xs font-semibold text-gray-500 mb-3">문서 청크 ({detail.chunks.length}개)</p>
                    <div className="space-y-2">
                      {detail.chunks.map((chunk) => (
                        <div key={chunk.id} className="bg-white rounded-lg border border-gray-200 px-3 py-2">
                          <p className="text-xs text-blue-500 font-medium mb-1">청크 #{chunk.chunkIndex}</p>
                          <p className="text-xs text-gray-600 line-clamp-3 leading-relaxed">{chunk.content}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
