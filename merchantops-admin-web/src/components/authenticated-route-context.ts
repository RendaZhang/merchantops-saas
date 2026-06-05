import { useOutletContext } from 'react-router-dom'

export type AuthenticatedRouteContext = {
  handleAuthenticationError: (error: unknown) => boolean
}

export function useAuthenticatedRoute() {
  return useOutletContext<AuthenticatedRouteContext>()
}
