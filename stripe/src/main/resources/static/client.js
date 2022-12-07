var PUBLISHABLE_KEY=""
var stripe = ""
var card = ""

//meta information for the routes
//fetch publishable key from server on load event
window.onload = async function () {
const language = routeParams[routeParams.length - 1];
const paymentMethod = routeParams[routeParams.length - 2];
const amount = routeParams[routeParams.length - 3];
const currency = routeParams[routeParams.length - 4];
const label = routeParams[routeParams.length - 6];
const provider = routeParams[routeParams.length - 7];
console.log(paymentMethod, amount, currency, provider, language, label);

await fetch(`/IPayment_1940/v1/${provider}/${label}/publishable-key/${language}`).
then(response => response.json()).
then(res => {
        data = res;
        PUBLISHABLE_KEY = data.publishableKey
        stripe = Stripe(PUBLISHABLE_KEY);
        var elements = stripe.elements();
        card = elements.create("card", { style: style });
        card.mount('#card-element');

});

}




const routeParams = window.location.pathname.split('/');
const btn =  document.querySelector('#submit')


const language = routeParams[routeParams.length - 1];
const paymentMethod = routeParams[routeParams.length - 2];
const amount = routeParams[routeParams.length - 3];
const currency = routeParams[routeParams.length - 4];
const label = routeParams[routeParams.length - 6];
const provider = routeParams[routeParams.length - 7];
console.log(paymentMethod, amount, currency, provider, language, label);




var style = {
  base: {
    color: "#32325d",
    height: "40px",
    width: "50%",
    lineHeight: '1.429'
  }
};


var form = document.getElementById('payment-form');
form.addEventListener('submit',  async (e) => {
  e.preventDefault()
    btn.disabled = true;
    btn.textContent = 'Processing...';
    btn.style.backgroundColor = 'grey';


//Here we fetch the client secret by sending amount, currency and payment method to the server
//The server will create a payment intent and return the client secret
//The client secret is used to confirm the payment
  const res = await  fetch(`/IPayment_1940/v1/${provider}/${label}/create-payment-intent/${language}`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json',},
    body: JSON.stringify({
        amount: amount,
        paymentMethod: [paymentMethod],
        currency: currency,
    })
})
//get input value of the customer. For this, it can be customizable,
//but for this example, we will use the name of the customer(We could add more information on what is needed)

const name = document.querySelector('#name').value
const email = document.querySelector('#email').value

const data = await res.json()
const clientSecret = data.clientSecret;
//Here we confirm the payment
//We pass the client secret and the payment method
//We also pass the billing

const paymentIntent = await stripe.confirmCardPayment(clientSecret, {
    payment_method: {
         card: card,
            billing_details: {
                name: name,
                email: email
            }
    }
}).then((result) => {
    if (result.error) {
     btn.disabled = false;
     btn.textContent = 'Pay';
     btn.style.removeProperty('background-color');
     document.querySelector('#card-errors').innerHTML = result.error.message;
      // Show error to your customer (e.g., insufficient funds)
      // The kind of error that could occur and how to ensure its updated.
      // https://stripe.com/docs/payments/accept-a-payment?platform=web&ui=elements#web-handle-errors
      console.log(result.error.message);

    } else {
      // The payment has been processed!
      if (result.paymentIntent.status === 'succeeded') {
      btn.textContent = 'Payment Successful';
      btn.style.backgroundColor = 'green';

        const root =  document.querySelector("#root")
         root.innerHTML = ` <div class="success alert">
                                   <div class="alert-body">
                                    Payment Successful !
                                   </div>
                               </div>`

        // Show a success message to your customer
        // There's a risk of the customer closing the window before callback
        // execution. Webhook is setup for that.('/webhook')
        // payment_intent.succeeded event that handles any business critical
        // post-payment actions.
      }
    }
    });

})

//