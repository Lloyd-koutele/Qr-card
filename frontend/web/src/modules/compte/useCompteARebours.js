import { useEffect, useState } from 'react'

/** Secondes restantes avant de pouvoir redemander un code. */
export function useCompteARebours() {
  const [restant, setRestant] = useState(0)
  useEffect(() => {
    if (restant <= 0) return undefined
    const minuterie = setTimeout(() => setRestant(restant - 1), 1000)
    return () => clearTimeout(minuterie)
  }, [restant])
  return [restant, setRestant]
}
