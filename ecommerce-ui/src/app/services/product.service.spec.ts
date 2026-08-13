import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { environment } from '../../../environments/environment';
import { Product } from '../models/product.model';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;
  const apiUrl = environment.apiUrl + '/products';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService]
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAllProducts()', () => {
    it('should fetch all products', (done) => {
      const mockProducts: Product[] = [
        { id: 1, name: 'Product 1', price: 99.99, category: 'Electronics', stock: 10 },
        { id: 2, name: 'Product 2', price: 149.99, category: 'Books', stock: 5 }
      ];

      service.getAllProducts().subscribe((products) => {
        expect(products).toEqual(mockProducts);
        expect(products.length).toBe(2);
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockProducts);
    });

    it('should handle empty product list', (done) => {
      service.getAllProducts().subscribe((products) => {
        expect(products).toEqual([]);
        expect(products.length).toBe(0);
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush([]);
    });

    it('should handle error when fetching products', (done) => {
      service.getAllProducts().subscribe({
        error: (error) => {
          expect(error.status).toBe(500);
          done();
        }
      });

      const req = httpMock.expectOne(apiUrl);
      req.flush({ message: 'Internal server error' }, { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getProductById()', () => {
    it('should fetch product by id', (done) => {
      const mockProduct: Product = { id: 1, name: 'Product 1', price: 99.99, category: 'Electronics', stock: 10 };

      service.getProductById(1).subscribe((product) => {
        expect(product).toEqual(mockProduct);
        expect(product.id).toBe(1);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockProduct);
    });

    it('should handle product not found', (done) => {
      service.getProductById(999).subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush({ message: 'Product not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('createProduct()', () => {
    it('should create new product', (done) => {
      const newProduct: Product = { id: 0, name: 'New Product', price: 199.99, category: 'Electronics', stock: 20 };
      const mockResponse: Product = { id: 3, name: 'New Product', price: 199.99, category: 'Electronics', stock: 20 };

      service.createProduct(newProduct).subscribe((product) => {
        expect(product.id).toBe(3);
        expect(product.name).toBe('New Product');
        done();
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newProduct);
      req.flush(mockResponse);
    });
  });

  describe('updateProduct()', () => {
    it('should update existing product', (done) => {
      const updatedProduct: Product = { id: 1, name: 'Updated Product', price: 129.99, category: 'Electronics', stock: 15 };

      service.updateProduct(1, updatedProduct).subscribe((product) => {
        expect(product.name).toBe('Updated Product');
        expect(product.price).toBe(129.99);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedProduct);
      req.flush(updatedProduct);
    });

    it('should handle update error for non-existent product', (done) => {
      const updatedProduct: Product = { id: 999, name: 'Updated Product', price: 129.99, category: 'Electronics', stock: 15 };

      service.updateProduct(999, updatedProduct).subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush({ message: 'Product not found' }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('deleteProduct()', () => {
    it('should delete product', (done) => {
      service.deleteProduct(1).subscribe(() => {
        expect(true).toBe(true);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });

    it('should handle delete error', (done) => {
      service.deleteProduct(999).subscribe({
        error: (error) => {
          expect(error.status).toBe(404);
          done();
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/999`);
      req.flush({ message: 'Product not found' }, { status: 404, statusText: 'Not Found' });
    });
  });
});
