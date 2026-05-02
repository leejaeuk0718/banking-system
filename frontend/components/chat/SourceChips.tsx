import { ExternalLink, FileText } from "lucide-react";
import type { SourceChip } from "@/types";

interface Props {
  sources: SourceChip[];
}

export default function SourceChips({ sources }: Props) {
  if (!sources || sources.length === 0) return null;
  return (
    <div className="mt-3 pt-3 border-t border-gray-100">
      <p className="text-xs text-gray-400 mb-2 font-medium">📚 참고 문서</p>
      <div className="flex flex-wrap gap-2">
        {sources.map((s, i) => (
          <div
            key={i}
            className="inline-flex items-center gap-1.5 bg-blue-50 border border-blue-100 text-blue-700 rounded-full px-3 py-1 text-xs"
            title={`${s.filename} — 청크 #${s.chunkIndex} (유사도: ${(s.score * 100).toFixed(1)}%)`}
          >
            <FileText size={11} />
            <span className="max-w-[180px] truncate">{s.source || s.filename}</span>
            <span className="text-blue-400">{(s.score * 100).toFixed(0)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}
