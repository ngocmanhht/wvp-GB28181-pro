const TokenKey = 'wvp-token'
const NameKey = 'wvp-username'
const ServerIdKey = 'wvp-server-id'

export function getToken(): string | null {
  return localStorage.getItem(TokenKey)
}

export function setToken(token: string): void {
  localStorage.setItem(TokenKey, token)
}

export function removeToken(): void {
  localStorage.removeItem(TokenKey)
}

export function getName(): string | null {
  return localStorage.getItem(NameKey)
}

export function setName(name: string): void {
  localStorage.setItem(NameKey, name)
}

export function removeName(): void {
  localStorage.removeItem(NameKey)
}

export function getServerId(): string | null {
  return localStorage.getItem(ServerIdKey)
}

export function setServerId(id: string): void {
  localStorage.setItem(ServerIdKey, id)
}

export function removeServerId(): void {
  localStorage.removeItem(ServerIdKey)
}

export function clearAuth(): void {
  removeToken()
  removeName()
  removeServerId()
}
