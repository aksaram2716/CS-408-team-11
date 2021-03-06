var express               =   require("express"),
    app                   =   express(),
    bodyParser            =   require("body-parser"),
    passport              =   require("passport"),
    bcrypt                =   require('bcrypt-nodejs'),
    morgan                =   require('morgan'),
    flash                 =   require('connect-flash');

require('./config/passport')(passport);
app.use(morgan('dev'));
app.use(bodyParser.urlencoded({extended : true}));
app.use(bodyParser.json());
app.set("view engine", "ejs");

app.use(require("express-session")({
  secret: "Shopper spelt without an e",
  resave: true,
	saveUninitialized: true
}));

app.use(passport.initialize());
app.use(passport.session());
app.use(flash());

require('./app/routes.js')(app, passport); // load our routes and pass in our app and fully configured passport

// connection.connect(function(err) {
//   if(err)
//     console.log(err);
// });

// connection.query('USE c9');	

app.use(express.static(__dirname + '/views'));

/*
* Routes to the landing page which is the index page
*/


// app.listen(process.env.PORT, process.env.IP, function() {
//     console.log("It has started bro");
// });

app.listen(3000);
