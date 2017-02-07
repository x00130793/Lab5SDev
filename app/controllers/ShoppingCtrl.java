package controllers;

import controllers.security.CheckIfCustomer;
import controllers.security.Secured;
import models.products.Product;
import models.shopping.Basket;
import models.shopping.OrderItem;
import models.shopping.ShopOrder;
import models.users.Customer;
import models.users.User;
import org.springframework.core.annotation.Order;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;
import views.html.*;

// Import models
// Import security controllers



public class ShoppingCtrl extends Controller {

    @Security.Authenticated(Secured.class)

    @With(CheckIfCustomer.class)

    // Get a user - if logged in email will be set in the session
	private Customer getCurrentUser() {
		return (Customer)User.getLoggedIn(session().get("email"));
	}

    @Transactional
    public Result addToBasket(Long id){
        Product p = Product.find.byId(id);

        Customer customer = (Customer)User.getLoggedIn(session().get("email"));

        if(customer.getBasket() == null){
            customer.setBasket(new Basket());
            customer.getBasket().setCustomer(customer);
            customer.update();
        }

        customer.getBasket().addProduct(p);
        customer.update();

        return ok(basket.render(customer));
    }

    @Transactional
    public Result showBasket(){
        return ok(basket.render(getCurrentUser()));
    }

    @Transactional
    public Result addOne(Long itemId){
        OrderItem item = OrderItem.find.byId(itemId);
        item.increaseQty();
        item.update();
        return redirect(routes.ShoppingCtrl.showBasket());
    }

    @Transactional
    public Result removeOne(Long itemId){
        OrderItem item = OrderItem.find.byId(itemId);
        Customer c = getCurrentUser();
        c.getBasket().removeItem(item);
        c.getBasket().update();
        return ok(basket.render(c));
    }
    
    @Transactional
    public Result placeOrder(){
        Customer c = getCurrentUser();

        ShopOrder order = new ShopOrder();

        order.setCustomer(c);

        order.setItems(c.getBasket().getBasketItems());

        order.save();

        for (OrderItem i: order.getItems()){
            i.setOrder(order);
            i.setBasket(null);
            i.update();
        }

        order.update();

        c.getBasket().setBasketItems(null);
        c.getBasket().update();

        return ok(orderConfirmed.render(c, order));
    }
    



    // Empty Basket
    @Transactional
    public Result emptyBasket() {
        
        Customer c = getCurrentUser();
        c.getBasket().removeAllItems();
        c.getBasket().update();
        
        return ok(basket.render(c));
    }


    
    // View an individual order
    @Transactional
    public Result viewOrder(long id) {
        ShopOrder order = ShopOrder.find.byId(id);
        return ok(orderConfirmed.render(getCurrentUser(), order));
    }

}