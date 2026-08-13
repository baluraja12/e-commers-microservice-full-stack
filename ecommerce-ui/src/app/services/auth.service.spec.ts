import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/users';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('login()', () => {
    it('should login successfully and store token', (done) => {
      const credentials = { username: 'testuser', password: 'password123' };
      const mockResponse = { token: 'jwt-token', userId: 1, username: 'testuser' };

      service.login(credentials).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        expect(localStorage.getItem('token')).toBe('jwt-token');
        expect(localStorage.getItem('currentUser')).toBe(JSON.stringify(mockResponse));
        done();
      });

      const req = httpMock.expectOne(apiUrl + '/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);
      req.flush(mockResponse);
    });

    it('should update currentUser subject on login', (done) => {
      const credentials = { username: 'testuser', password: 'password123' };
      const mockResponse = { token: 'jwt-token', userId: 1, username: 'testuser' };

      service.currentUser.subscribe((user) => {
        if (user) {
          expect(user).toEqual(mockResponse);
          done();
        }
      });

      service.login(credentials).subscribe();
      const req = httpMock.expectOne(apiUrl + '/login');
      req.flush(mockResponse);
    });

    it('should handle login failure', (done) => {
      const credentials = { username: 'testuser', password: 'wrongpassword' };

      service.login(credentials).subscribe({
        error: (error) => {
          expect(error.status).toBe(401);
          done();
        }
      });

      const req = httpMock.expectOne(apiUrl + '/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('register()', () => {
    it('should register new user', (done) => {
      const user = { username: 'newuser', email: 'test@example.com', password: 'password123' };
      const mockResponse = { message: 'User registered successfully' };

      service.register(user).subscribe((response) => {
        expect(response).toEqual(mockResponse);
        done();
      });

      const req = httpMock.expectOne(apiUrl + '/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(user);
      req.flush(mockResponse);
    });

    it('should handle registration error', (done) => {
      const user = { username: 'existinguser', email: 'existing@example.com', password: 'password123' };

      service.register(user).subscribe({
        error: (error) => {
          expect(error.status).toBe(409);
          done();
        }
      });

      const req = httpMock.expectOne(apiUrl + '/register');
      req.flush({ message: 'User already exists' }, { status: 409, statusText: 'Conflict' });
    });
  });

  describe('logout()', () => {
    it('should clear token and user data', () => {
      localStorage.setItem('token', 'jwt-token');
      localStorage.setItem('currentUser', JSON.stringify({ userId: 1, username: 'testuser' }));

      service.logout();

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('currentUser')).toBeNull();
    });

    it('should reset currentUser subject to null', (done) => {
      localStorage.setItem('token', 'jwt-token');
      localStorage.setItem('currentUser', JSON.stringify({ userId: 1, username: 'testuser' }));

      service.logout();

      service.currentUser.subscribe((user) => {
        expect(user).toBeNull();
        done();
      });
    });
  });

  describe('getToken()', () => {
    it('should return token from localStorage', () => {
      localStorage.setItem('token', 'my-jwt-token');
      expect(service.getToken()).toBe('my-jwt-token');
    });

    it('should return null when no token exists', () => {
      expect(service.getToken()).toBeNull();
    });
  });

  describe('isLoggedIn()', () => {
    it('should return true when token exists', () => {
      localStorage.setItem('token', 'jwt-token');
      expect(service.isLoggedIn()).toBe(true);
    });

    it('should return false when no token', () => {
      expect(service.isLoggedIn()).toBe(false);
    });
  });

  describe('currentUserValue', () => {
    it('should return current user from subject', () => {
      const user = { userId: 1, username: 'testuser' };
      localStorage.setItem('currentUser', JSON.stringify(user));
      service = TestBed.inject(AuthService);
      expect(service.currentUserValue).toEqual(user);
    });
  });
});
