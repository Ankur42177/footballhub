import ReactMarkdown from "react-markdown";
import type { Components } from "react-markdown";
import remarkGfm from "remark-gfm";

/**
 * Renders user-written Markdown safely: raw HTML is dropped (skipHtml) and react-markdown's
 * default URL filter strips javascript: and similar links. Headings shift down one level so
 * the article title stays the page's only <h1>.
 *
 * Images are allowed only from Wikimedia Commons (freely licensed, and the licence asks for
 * credit), and render as a captioned figure. The Markdown title carries the caption, the
 * credit and the file page, separated by "||":
 *
 *   ![Alt text](https://upload.wikimedia.org/... "Caption||Photo: Name, CC BY-SA 4.0||https://commons.wikimedia.org/wiki/File:...")
 */
const IMAGE_HOSTS = ["https://upload.wikimedia.org/", "https://thumb.wikimedia.org/"];
const CREDIT_HOST = "https://commons.wikimedia.org/";

function Figure({ src, alt, title }: { src?: string; alt?: string; title?: string }) {
  if (typeof src !== "string" || !IMAGE_HOSTS.some((host) => src.startsWith(host))) return null;
  const [caption, credit, page] = (title ?? "").split("||").map((part) => part.trim());
  const creditLink = page && page.startsWith(CREDIT_HOST) ? page : undefined;
  return (
    <figure className="article-figure">
      {/* Remote, already-sized Commons thumbnails; next/image would add nothing here. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={src} alt={alt ?? ""} loading="lazy" decoding="async" referrerPolicy="no-referrer" />
      {(caption || credit) && (
        <figcaption>
          {caption && <span>{caption}</span>}
          {credit && (
            <span className="article-figure-credit">
              {creditLink ? (
                <a href={creditLink} rel="nofollow noopener noreferrer" target="_blank">
                  {credit}
                </a>
              ) : (
                credit
              )}
            </span>
          )}
        </figcaption>
      )}
    </figure>
  );
}

const components: Components = {
  h1: ({ children }) => <h2>{children}</h2>,
  a: ({ href, title, children }) => (
    <a href={href} title={title} rel="nofollow ugc noopener noreferrer">
      {children}
    </a>
  ),
  // Wide tables scroll sideways on phones instead of stretching the page.
  table: ({ children }) => (
    <div className="table-scroll">
      <table>{children}</table>
    </div>
  ),
  img: ({ src, alt, title }) => <Figure src={typeof src === "string" ? src : undefined} alt={alt} title={title} />,
  // A paragraph holding only an image becomes a bare <figure> (a figure can't sit inside <p>).
  p: ({ node, children }) => {
    const only = node?.children.filter((child) => !(child.type === "text" && !child.value.trim()));
    if (only?.length === 1 && only[0].type === "element" && only[0].tagName === "img") return <>{children}</>;
    return <p>{children}</p>;
  },
};

export function Markdown({ source, className = "article-body" }: { source: string; className?: string }) {
  return (
    <div className={className}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} skipHtml components={components}>
        {source}
      </ReactMarkdown>
    </div>
  );
}
