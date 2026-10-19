import service from '@/utils/request'
import { AxiosInstance, AxiosRequestConfig } from 'axios'

export abstract class BaseService {
  protected http: AxiosInstance = service

  protected get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.http.get(url, { params, ...config }) as unknown as Promise<T>
  }

  protected post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.http.post(url, data, config) as unknown as Promise<T>
  }

  protected put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.http.put(url, data, config) as unknown as Promise<T>
  }

  protected delete<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.http.delete(url, { params, ...config }) as unknown as Promise<T>
  }
}
