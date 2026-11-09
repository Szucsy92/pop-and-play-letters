import { createFileRoute } from "@tanstack/react-router";
import { KeypadGame } from "@/components/KeypadGame";
export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Gyerek billentyűzet – színes betűk és hangok" },
      { name: "description", content: "Teljes képernyős, biztonságos gyerek billentyűzet nagy színes betűkkel, konfettivel és lágy hangokkal." },
      { property: "og:title", content: "Gyerek billentyűzet" },
      { property: "og:description", content: "Nagy, színes betűk, konfetti és xilofon hangok – kicsiknek." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: KeypadGame,
});

