package org.cartdetails.details.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.cartdetails.details.entity.Order;
import org.cartdetails.details.entity.OrderDetail;
import org.cartdetails.details.entity.Product;
import org.cartdetails.details.model.CartInfo;
import org.cartdetails.details.model.CartLineInfo;
import org.cartdetails.details.model.CustomerInfo;
import org.cartdetails.details.model.OrderDetailInfo;
import org.cartdetails.details.model.OrderInfo;
import org.cartdetails.details.pagination.PaginationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
public class OrderDAO {

	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private ProductDAO productDAO;
	
	private int getMaxOrderNum() {
		
		String sql = "Select max(ordernum) from "+ Order.class.getName();
		Session session = this.sessionFactory.getCurrentSession();
		Query<Integer> query = session.createQuery(sql, Integer.class);
		Integer value = (Integer)query.getSingleResult();
		if(value == null) {
			return 0;
		}
		return value;
		
	}
	
	@Transactional(rollbackFor=Exception.class)
	public void saveOrder(CartInfo cartInfo) {
		Session session = this.sessionFactory.getCurrentSession();
		
		int orderNum = this.getMaxOrderNum() + 1;
		Order order = new Order();
		
		order.setId(UUID.randomUUID().toString());
		order.setOrderNum(orderNum);
		order.setOrderDate(new Date());
		order.setAmount(cartInfo.getAmountTotal());
		
		CustomerInfo customerInfo = cartInfo.getCustomerInfo();
		order.setCustomerName(customerInfo.getName());
		order.setCustomerEmail(customerInfo.getEmail());
	    order.setCustomerPhone(customerInfo.getPhone());
	    order.setCustomerAddress(customerInfo.getAddress());
	    
	    session.persist(order);
	    
	    List<CartLineInfo> lines = cartInfo.getCartLines();
	    
	    for(CartLineInfo line : lines) {
	    	OrderDetail detail = new OrderDetail();
	    	detail.setId(UUID.randomUUID().toString());
	    	detail.setOrder(order);
	    	detail.setAmount(line.getAmount());
	    	detail.setPrice(line.getProductInfo().getPrice());
	    	detail.setQuantity(line.getQuantity());
	    	
	    	String code = line.getProductInfo().getCode();
	    	Product product = this.productDAO.findProduct(code);
	    	detail.setProduct(product);
	    	
	    	session.persist(detail);
	    }
	    
	    cartInfo.setOrderNum(orderNum);
	    session.flush();
	}
	
	public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage){
		
		String sql="Select new "+ OrderInfo.class.getName()//
				+ "(id, orderdate, ordernum, amount,"//
				+ " customername, customeraddress, customeremail, customerphone) from "//
				+ Order.class.getName() + " order by ordernum desc";
		
		Session session = this.sessionFactory.getCurrentSession();
		Query<OrderInfo> query = session.createQuery(sql, OrderInfo.class);
		return new PaginationResult<OrderInfo>(query, page, maxResult, maxNavigationPage);
	}
	
	public Order findOrder(String orderId) {
		Session session = this.sessionFactory.getCurrentSession();
		return session.find(Order.class, orderId);
	}
	
	public OrderInfo getOrderInfo(String orderId) {
		Order order = this.findOrder(orderId);
		if(order == null) {
			return null;
		}
		return new OrderInfo(order.getId(), order.getOrderDate(), //
				order.getOrderNum(), order.getAmount(), order.getCustomerName(), //
				order.getCustomerAddress(), order.getCustomerEmail(), order.getCustomerPhone());
	}
	
	public List<OrderDetailInfo> listOrderDetailInfos(String orderId){
		String sql = "Select new "+ OrderDetailInfo.class.getName()//
				+ "(id, product.code, product.name, quantity, price, amount) from "//
				+ OrderDetail.class.getName() + " where order.id = :orderId";
		
		Session session = this.sessionFactory.getCurrentSession();
		Query<OrderDetailInfo> query = session.createQuery(sql, OrderDetailInfo.class);
		query.setParameter("orderId", orderId);
		
		return query.getResultList();
	}
}
