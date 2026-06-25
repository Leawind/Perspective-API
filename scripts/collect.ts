import "@std/dotenv/load";
import {
  collectFiles,
  DEFAULT_IGNORE_PATTERNS,
  formatCollected,
} from "@leawind/inventory/collect-files";

const files = await collectFiles(".", {
  ignorePatterns: [
    "gradlew",
    "gradlew.bat",
	"src/test/**",
    ...DEFAULT_IGNORE_PATTERNS,
  ],
});

const output = formatCollected(files);

const OUTPUT_FILE = Deno.env.get('COLLECT_OUTPUT') || 'build/src.txt'

Deno.writeTextFileSync(OUTPUT_FILE, output, { create: true });
