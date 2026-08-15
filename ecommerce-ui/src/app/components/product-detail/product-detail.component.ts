import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-detail.component.html'
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : 0;
    
    if (id > 0) {
      this.loadProduct(id);
    } else {
      this.error = 'Invalid product ID.';
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  loadProduct(id: number): void {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges();

    this.productService.getProductById(id).subscribe({
      next: (data: any) => {
        this.product = data;
        this.loading = false;
        this.cdr.detectChanges(); // FORCE UI UPDATE
      },
      error: (err: any) => {
        this.error = 'Failed to load product.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  addToCart(): void {
    if (!this.product) return;
    let cart = JSON.parse(localStorage.getItem('cart') || '[]');
    const existing = cart.find((item: any) => item.product.id === this.product!.id);
    if (existing) {
      existing.quantity++;
    } else {
      cart.push({ product: this.product, quantity: 1 });
    }
    localStorage.setItem('cart', JSON.stringify(cart));
    alert(this.product.name + ' added to cart!');
  }
}