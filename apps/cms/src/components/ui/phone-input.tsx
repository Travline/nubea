import * as React from "react"
import {
  AsYouType,
  type CountryCode,
  getCountries,
  getCountryCallingCode,
} from "libphonenumber-js"
import { ChevronDown, Check, Search } from "lucide-react"
import { cn } from "@/lib/utils"

function getCountryFlag(countryCode: string) {
  if (!countryCode || countryCode.length !== 2) return "🌐"
  return countryCode
    .toUpperCase()
    .replace(/./g, (char) =>
      String.fromCodePoint(char.charCodeAt(0) + 127397)
    )
}

const displayNames = new Intl.DisplayNames(["es"], { type: "region" })

interface CountryItem {
  code: CountryCode
  name: string
  dialCode: string
  flag: string
}

const PRIORITY_COUNTRIES: CountryCode[] = [
  "PE",
  "MX",
  "CO",
  "AR",
  "CL",
  "ES",
  "US",
  "EC",
  "BO",
  "VE",
  "BR",
  "UY",
  "PY",
  "CR",
  "PA",
  "DO",
  "GT",
]

const ALL_COUNTRIES: CountryItem[] = (() => {
  const prioritySet = new Set(PRIORITY_COUNTRIES)
  const priorityList: CountryItem[] = []
  const othersList: CountryItem[] = []

  for (const code of PRIORITY_COUNTRIES) {
    try {
      priorityList.push({
        code,
        name: displayNames.of(code) || code,
        dialCode: `+${getCountryCallingCode(code)}`,
        flag: getCountryFlag(code),
      })
    } catch {
      // ignore
    }
  }

  for (const code of getCountries()) {
    if (!prioritySet.has(code)) {
      try {
        othersList.push({
          code,
          name: displayNames.of(code) || code,
          dialCode: `+${getCountryCallingCode(code)}`,
          flag: getCountryFlag(code),
        })
      } catch {
        // ignore
      }
    }
  }

  othersList.sort((a, b) => a.name.localeCompare(b.name, "es"))
  return [...priorityList, ...othersList]
})()

export interface PhoneInputProps
  extends Omit<React.ComponentProps<"input">, "onChange" | "value" | "defaultValue"> {
  value?: string
  defaultValue?: string
  onChange?: (value: string) => void
  defaultCountry?: CountryCode
  containerClassName?: string
}

export const PhoneInput = React.forwardRef<HTMLInputElement, PhoneInputProps>(
  (
    {
      className,
      containerClassName,
      defaultCountry = "PE",
      value: controlledValue,
      defaultValue = "",
      onChange,
      disabled,
      placeholder,
      id,
      ...props
    },
    ref
  ) => {
    const [country, setCountry] = React.useState<CountryCode>(defaultCountry)
    const [internalValue, setInternalValue] = React.useState(defaultValue)
    const [isOpen, setIsOpen] = React.useState(false)
    const [search, setSearch] = React.useState("")

    const isControlled = controlledValue !== undefined
    const rawValue = isControlled ? controlledValue : internalValue

    const dropdownRef = React.useRef<HTMLDivElement>(null)
    const searchInputRef = React.useRef<HTMLInputElement>(null)
    const phoneInputRef = React.useRef<HTMLInputElement | null>(null)

    // Sync input ref
    React.useImperativeHandle(ref, () => phoneInputRef.current as HTMLInputElement)

    // Close dropdown on outside click
    React.useEffect(() => {
      if (!isOpen) return

      const handleClickOutside = (e: MouseEvent | TouchEvent) => {
        if (
          dropdownRef.current &&
          !dropdownRef.current.contains(e.target as Node)
        ) {
          setIsOpen(false)
        }
      }

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === "Escape") {
          setIsOpen(false)
          phoneInputRef.current?.focus()
        }
      }

      document.addEventListener("mousedown", handleClickOutside)
      document.addEventListener("touchstart", handleClickOutside)
      document.addEventListener("keydown", handleKeyDown)

      return () => {
        document.removeEventListener("mousedown", handleClickOutside)
        document.removeEventListener("touchstart", handleClickOutside)
        document.removeEventListener("keydown", handleKeyDown)
      }
    }, [isOpen])

    // Focus search input when dropdown opens
    React.useEffect(() => {
      if (isOpen) {
        setSearch("")
        setTimeout(() => searchInputRef.current?.focus(), 50)
      }
    }, [isOpen])

    // Format value with libphonenumber-js
    const formatNumber = React.useCallback(
      (input: string, cCode: CountryCode) => {
        const asYouType = new AsYouType(cCode)
        const formatted = asYouType.input(input)
        const detectedCountry = asYouType.getCountry()
        return { formatted, detectedCountry }
      },
      []
    )

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      const val = e.target.value
      const { formatted, detectedCountry } = formatNumber(val, country)

      if (detectedCountry && detectedCountry !== country) {
        setCountry(detectedCountry)
      }

      if (!isControlled) {
        setInternalValue(formatted)
      }
      onChange?.(formatted)
    }

    const handleCountrySelect = (newCountry: CountryCode) => {
      setCountry(newCountry)
      setIsOpen(false)

      // Reformat existing value if present
      if (rawValue) {
        const digits = rawValue.replace(/\D/g, "")
        const { formatted } = formatNumber(digits, newCountry)
        if (!isControlled) {
          setInternalValue(formatted)
        }
        onChange?.(formatted)
      }

      phoneInputRef.current?.focus()
    }

    const currentCountryDial = React.useMemo(() => {
      try {
        return `+${getCountryCallingCode(country)}`
      } catch {
        return "+51"
      }
    }, [country])

    const filteredCountries = React.useMemo(() => {
      if (!search.trim()) return ALL_COUNTRIES
      const q = search.toLowerCase().trim()
      return ALL_COUNTRIES.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          c.dialCode.includes(q) ||
          c.code.toLowerCase().includes(q)
      )
    }, [search])

    // Compute sample placeholder based on country
    const computedPlaceholder = React.useMemo(() => {
      if (placeholder) return placeholder
      if (country === "PE") return "912 345 678"
      if (country === "MX") return "55 1234 5678"
      if (country === "US") return "(555) 123-4567"
      if (country === "CO") return "300 123 4567"
      if (country === "AR") return "11 1234-5678"
      if (country === "ES") return "612 34 56 78"
      return "Número de teléfono"
    }, [placeholder, country])

    return (
      <div
        ref={dropdownRef}
        className={cn(
          "relative flex h-8 w-full min-w-0 items-center rounded-lg border border-input bg-transparent text-base transition-colors md:text-sm dark:bg-input/30",
          "focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50",
          disabled && "pointer-events-none cursor-not-allowed opacity-50 bg-input/50 dark:bg-input/80",
          containerClassName
        )}
      >
        {/* Country selector trigger */}
        <button
          type="button"
          disabled={disabled}
          onClick={() => setIsOpen((prev) => !prev)}
          className={cn(
            "inline-flex h-full items-center gap-1.5 rounded-l-lg border-r border-input bg-transparent px-2.5 py-1 text-xs font-medium text-foreground transition-colors hover:bg-muted/60 focus-visible:outline-none select-none cursor-pointer",
            isOpen && "bg-muted/70"
          )}
          aria-label="Seleccionar país"
          aria-expanded={isOpen}
          aria-haspopup="listbox"
        >
          <span className="text-base leading-none" role="img" aria-label={country}>
            {getCountryFlag(country)}
          </span>
          <span className="text-xs font-semibold text-muted-foreground">
            {currentCountryDial}
          </span>
          <ChevronDown
            className={cn(
              "size-3 text-muted-foreground transition-transform duration-200",
              isOpen && "rotate-180"
            )}
          />
        </button>

        {/* Phone text input */}
        <input
          ref={phoneInputRef}
          id={id}
          type="tel"
          disabled={disabled}
          value={rawValue}
          onChange={handleInputChange}
          placeholder={computedPlaceholder}
          className={cn(
            "h-full w-full min-w-0 bg-transparent px-2.5 py-1 text-base text-foreground outline-none placeholder:text-muted-foreground md:text-sm",
            className
          )}
          {...props}
        />

        {/* Dropdown Menu */}
        {isOpen && (
          <div
            className="absolute top-full left-0 z-50 mt-1.5 w-72 rounded-lg border border-border bg-popover p-1.5 text-popover-foreground shadow-lg animate-in fade-in-0 zoom-in-95"
            role="listbox"
          >
            {/* Search filter */}
            <div className="flex items-center gap-2 rounded-md border border-input bg-background/50 px-2 py-1 mb-1.5">
              <Search className="size-3.5 text-muted-foreground shrink-0" />
              <input
                ref={searchInputRef}
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Buscar país o código..."
                className="w-full bg-transparent text-xs text-foreground outline-none placeholder:text-muted-foreground"
              />
            </div>

            {/* Countries list */}
            <div className="max-h-56 overflow-y-auto scrollbar-thin divide-y divide-border/20">
              {filteredCountries.length === 0 ? (
                <div className="py-4 text-center text-xs text-muted-foreground">
                  No se encontraron países
                </div>
              ) : (
                filteredCountries.map((c) => {
                  const isSelected = c.code === country
                  return (
                    <button
                      key={c.code}
                      type="button"
                      onClick={() => handleCountrySelect(c.code)}
                      className={cn(
                        "flex w-full items-center justify-between gap-2 rounded-md px-2 py-1.5 text-left text-xs transition-colors hover:bg-accent hover:text-accent-foreground cursor-pointer",
                        isSelected && "bg-accent/80 font-medium text-accent-foreground"
                      )}
                      role="option"
                      aria-selected={isSelected}
                    >
                      <div className="flex items-center gap-2 truncate">
                        <span className="text-sm leading-none">{c.flag}</span>
                        <span className="truncate">{c.name}</span>
                        <span className="text-[10px] text-muted-foreground">({c.code})</span>
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0">
                        <span className="text-xs font-mono text-muted-foreground">
                          {c.dialCode}
                        </span>
                        {isSelected && <Check className="size-3.5 text-primary" />}
                      </div>
                    </button>
                  )
                })
              )}
            </div>
          </div>
        )}
      </div>
    )
  }
)

PhoneInput.displayName = "PhoneInput"
