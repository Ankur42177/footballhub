/** Response and request contracts mirroring the Spring Boot DTOs. */

export type Role = "USER" | "MODERATOR" | "ADMIN";
export type BlogStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type CommentStatus = "VISIBLE" | "HIDDEN" | "REMOVED";
export type ReportStatus = "OPEN" | "REVIEWING" | "RESOLVED" | "DISMISSED";
export type NotificationType = "COMMENT_ON_BLOG" | "REPLY_TO_COMMENT" | "MODERATION";

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  timestamp: string;
  fieldErrors: { field: string; message: string }[];
}

export interface Me {
  id: number;
  username: string;
  email: string;
  displayName: string;
  bio: string | null;
  role: Role;
  emailVerified: boolean;
  createdAt: string;
}

export interface AuthorSummary {
  username: string;
  displayName: string;
}

export interface ClubSummary {
  id: number;
  name: string;
  slug: string;
  shortName: string | null;
  primaryColor: string | null;
}

export interface Club extends ClubSummary {
  league: string;
  description: string | null;
  followerCount: number;
  postCount: number;
  followedByMe: boolean;
}

export interface BlogSummary {
  id: number;
  title: string;
  slug: string;
  excerpt: string | null;
  author: AuthorSummary;
  club: ClubSummary | null;
  status: BlogStatus;
  hidden: boolean;
  voteCount: number;
  commentCount: number;
  readingMinutes: number;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  votedByMe: boolean;
  bookmarkedByMe: boolean;
}

export interface BlogDetail extends BlogSummary {
  content: string;
  canEdit: boolean;
}

export interface Comment {
  id: number;
  blogId: number;
  parentId: number | null;
  author: AuthorSummary | null;
  content: string | null;
  status: CommentStatus;
  edited: boolean;
  createdAt: string;
  updatedAt: string;
  canEdit: boolean;
  canDelete: boolean;
  replies: Comment[];
}

export interface PublicProfile {
  username: string;
  displayName: string;
  bio: string | null;
  role: Role;
  joinedAt: string;
  publishedPosts: number;
  totalVotes: number;
}

export interface VoteResult {
  blogId: number;
  voteCount: number;
  voted: boolean;
}

export interface BookmarkResult {
  blogId: number;
  bookmarked: boolean;
}

export interface FollowResult {
  clubId: number;
  following: boolean;
  followerCount: number;
}

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface ReportTarget {
  type: "BLOG" | "COMMENT";
  blogId: number;
  blogSlug: string;
  blogTitle: string;
  blogHidden: boolean;
  commentId: number | null;
  commentExcerpt: string | null;
  commentStatus: CommentStatus | null;
  contentAuthor: string;
}

export interface Report {
  id: number;
  status: ReportStatus;
  reason: string;
  reporter: string;
  createdAt: string;
  target: ReportTarget;
  resolvedBy: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
}

export interface AdminUser {
  id: number;
  username: string;
  email: string;
  displayName: string;
  role: Role;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
}

export interface AuditLogEntry {
  id: number;
  actor: string;
  action: string;
  targetType: string;
  targetId: number | null;
  details: string | null;
  createdAt: string;
}

export interface BlogInput {
  title: string;
  excerpt: string;
  content: string;
  clubId: number | null;
}
