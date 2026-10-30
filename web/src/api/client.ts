import axios, { AxiosError } from 'axios';

export const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request interceptor: attach JWT bearer token if available
apiClient.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('fixloop_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: extract detailed server messages & handle 401s
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; error?: string }>) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('fixloop_token');
      sessionStorage.removeItem('fixloop_user');
      // If not already on login page, redirect or trigger re-auth
      if (window.location.pathname !== '/login') {
        window.dispatchEvent(new Event('fixloop:unauthorized'));
      }
    }

    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      'An unexpected network error occurred';

    return Promise.reject(new Error(message));
  }
);

export default apiClient;
