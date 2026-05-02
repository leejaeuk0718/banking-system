// ── Auth ──────────────────────────────────────────────────────
export interface UserResponse {
  id: number;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

// ── Chat ──────────────────────────────────────────────────────
export interface SourceChip {
  source: string;
  filename: string;
  chunkIndex: number;
  score: number;
}

export interface ChatRequest {
  conversationId?: number | null;
  message: string;
}

export interface ChatResponse {
  conversationId: number;
  conversationTitle: string;
  messageId: number;
  content: string;
  createdAt: string;
  sources: SourceChip[];
}

export interface MessageResponse {
  id: number;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
  sources?: SourceChip[];
}

export interface ConversationSummaryResponse {
  id: number;
  title: string;
  lastMessageAt: string;
}

export interface ConversationDetailResponse {
  id: number;
  title: string;
  messages: MessageResponse[];
}

// ── Bookmark ──────────────────────────────────────────────────
export interface BookmarkResponse {
  id: number;
  messageId: number;
  content: string;
  conversationTitle: string;
  createdAt: string;
}

export interface BookmarkRequest {
  messageId: number;
}

// ── Document ──────────────────────────────────────────────────
export type DocumentStatus = "PROCESSING" | "READY" | "FAILED";

export interface DocumentResponse {
  id: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  source: string;
  status: DocumentStatus;
  chunkCount: number;
  errorMessage?: string;
  createdAt: string;
}

export interface DocumentChunkResponse {
  id: number;
  chunkIndex: number;
  content: string;
}

export interface DocumentDetailResponse {
  id: number;
  originalFilename: string;
  source: string;
  status: DocumentStatus;
  createdAt: string;
  chunks: DocumentChunkResponse[];
}
