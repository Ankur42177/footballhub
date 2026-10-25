import ReactMarkdown from "react-markdown";
import type { Components } from "react-markdown";
import remarkGfm from "remark-gfm";

/**
 * Renders user-written Markdown safely: raw HTML is dropped (skipHtml), images are
 * not allowed (no licensed media and no tracking pixels), and react-markdown's default
 * URL filter strips javascript: and similar links. Headings shift down one level so
 * the article title stays the page's only <h1>.
 */
const components: Components = {
  h1: ({ children }) => <h2>{children}</h2>,
  a: ({ href, title, children }) => (
    <a href={href} title={title} rel="nofollow ugc noopener noreferrer">
      {children}
    </a>
  ),
};

export function Markdown({ source, className = "article-body" }: { source: string; className?: string }) {
  return (
    <div className={className}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        skipHtml
        disallowedElements={["img"]}
        unwrapDisallowed
        components={components}
      >
        {source}
      </ReactMarkdown>
    </div>
  );
}
