import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { environment } from '../../../environments/environment';
import { Order } from '../models/order.model';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/orders';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService]
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('getAllOrders()', () => {
    it('should fetch all orders', (done) => {
      const mockOrders: Order[] = [
        { id: 1, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] },
        { id: 2, userId: 1, totalAmount: 149.99, status: 'CONFIRMED', items: [] }
      ];

      service.getAllOrders().subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
        expect(orders.length).toBe(2);
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrders);
    });

    it('should handle empty order list', (done) => {
      service.getAllOrders().subscribe((orders) => {
        expect(orders).toEqual([]);
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush([]);
    });
  });

  describe('getOrderById()', () => {
    it('should fetch order by id', (done) => {
      const mockOrder: Order = { id: 1, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] };

      service.getOrderById(1).subscribe((order) => {
        expect(order).toEqual(mockOrder);
        expect(order.id).toBe(1);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });

    it('should handle order not found', (done) => {
      service.getOrderById(999).subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush({ message: 'Order not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getMyOrders()', () => {
    it('should fetch user orders from localStorage userId', (done) => {
      const user = { userId: 1, username: 'testuser' };
      localStorage.setItem('currentUser', JSON.stringify(user));

      const mockOrders: Order[] = [
        { id: 1, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] }
      ];

      service.getMyOrders().subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/user/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrders);
    });

    it('should default to userId 0 when no user in localStorage', (done) => {
      const mockOrders: Order[] = [];

      service.getMyOrders().subscribe((orders) => {
        expect(orders).toEqual([]);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/user/0`);
      req.flush([]);
    });
  });

  describe('createOrder()', () => {
    it('should create new order', (done) => {
      const newOrder: Order = { id: 0, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] };
      const mockResponse: Order = { id: 5, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] };

      service.createOrder(newOrder).subscribe((order) => {
        expect(order.id).toBe(5);
        expect(order.status).toBe('PENDING');
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newOrder);
      req.flush(mockResponse);
    });

    it('should handle order creation error', (done) => {
      const newOrder: Order = { id: 0, userId: 1, totalAmount: 299.99, status: 'PENDING', items: [] };

      service.createOrder(newOrder).subscribe({
        error: (error) => {
          expect(error.status).toBe(400);
          done();
        }
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush({ message: 'Invalid order' }, { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('updateStatus()', () => {
    it('should update order status', (done) => {
      const updatedOrder: Order = { id: 1, userId: 1, totalAmount: 299.99, status: 'SHIPPED', items: [] };

      service.updateStatus(1, 'SHIPPED').subscribe((order) => {
        expect(order.status).toBe('SHIPPED');
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/1/status`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'SHIPPED' });
      req.flush(updatedOrder);
    });

    it('should handle status update error', (done) => {
      service.updateStatus(999, 'SHIPPED').subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/999/status`);
      req.flush({ message: 'Order not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('getUserId()', () => {
    it('should extract userId from localStorage', () => {
      const user = { userId: 123, username: 'testuser' };
      localStorage.setItem('currentUser', JSON.stringify(user));
      
      // Access private method through any type cast
      const userId = (service as any).getUserId();
      expect(userId).toBe(123);
    });

    it('should return 0 when no user in localStorage', () => {
      const userId = (service as any).getUserId();
      expect(userId).toBe(0);
    });
  });
});
