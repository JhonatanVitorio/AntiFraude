import { useState } from "react";
import type { Theme } from "../types";

export function useTheme(initial: Theme = "dark") {
    const [theme, setTheme] = useState<Theme>(initial);

    const toggleTheme = () => {
        setTheme((prev) => (prev === "dark" ? "light" : "dark"));
    };

    const isDark = theme === "dark";

    return { theme, isDark, toggleTheme };
}
