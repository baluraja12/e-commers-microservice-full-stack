import { Product } from './product.model';

export interface OrderItem {
  productId: number;
  productName?: string;
  quantity: number;
  unitPrice?: number;
  subtotal?: number;
}

export interface Order {
  id?: number;
  userId?: number;
  totalAmount?: number;
  status?: string;
  orderItems: OrderItem[];
  createdAt?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}