# Widgets [Test]

REST Api service for widgets manipulating. 

## WidgetController
Provide REST API methods:
* GET /widget						Returns collection of all widgets
* GET /widget?page					Returns page of widgets
* GET /widget/filter?x1&y1&x2&y2		Returns widgets that fall entirely into the region settled by corners (x1,y1):(x2,y2)
* GET /widget/{widgetId}				Return widget descriptor
* POST /widget						Create new widget and return it
* PATCH /widget/{widgetId}			Update widget and return it
* DELETE /widget/{widgetId}			Delete widget and return it
* 
## WidgetService
Provide logic of validation and z-coordinate "shifting"

## WidgetRepository
Provide storage for widgets. Has 2 implementations:
* CustomInMemory
* H2
You can choose one of them in **application.properties** (widgets.repository.mode)
