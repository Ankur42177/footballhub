"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

const RETRY_SECONDS = 10;
const MAX_AUTO_RETRIES = 12; // about two minutes, longer than a cold start of the API

/**
 * Most errors here mean the API is asleep or still starting (free hosting sleeps when
 * idle). Retry on a countdown so the page recovers by itself once the API is up.
 */
export default function ErrorPage({ error, retry }: { error: unknown; retry: () => void }) {
  const [attempt, setAttempt] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(RETRY_SECONDS);
  const autoRetrying = attempt < MAX_AUTO_RETRIES;

  useEffect(() => {
    console.error(error);
  }, [error]);

  // One-second countdown; when it reaches zero, retry and start the next round.
  useEffect(() => {
    if (!autoRetrying) return;
    const timer = window.setTimeout(() => {
      if (secondsLeft > 1) {
        setSecondsLeft(secondsLeft - 1);
      } else {
        setAttempt((count) => count + 1);
        setSecondsLeft(RETRY_SECONDS);
        retry();
      }
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [autoRetrying, secondsLeft, retry]);

  return (
    <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6">
      <h1 className="display text-5xl">{autoRetrying ? "Warming up the floodlights" : "We lost the signal"}</h1>
      <p className="mt-4 text-lg text-chalk-dim">
        {autoRetrying
          ? "The server sleeps when nobody has visited for a while and takes about a minute to start. This page will load by itself as soon as it's ready."
          : "The server still isn't answering. Try again in a minute, or go back to the homepage."}
      </p>
      {autoRetrying && (
        <p className="mt-6 text-chalk-dim" role="status" aria-live="polite">
          Trying again in <span className="score text-2xl text-floodlight">{secondsLeft}</span> s
          {attempt > 0 && ` (attempt ${attempt + 1})`}
        </p>
      )}
      <div className="mt-8 flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => {
            setSecondsLeft(RETRY_SECONDS);
            setAttempt((count) => count + 1);
            retry();
          }}
          className="btn btn-primary"
        >
          Try now
        </button>
        <Link href="/" className="btn btn-secondary">
          Back to the homepage
        </Link>
      </div>
    </div>
  );
}
