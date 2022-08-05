        Halite Bot - Team First Force, 2022
    
            Team members:
            - Bloțiu Mihnea, 323CA
            - Podaru Andrei-Alexandru, 323CA
            - Roșu Mihai, 323CA


1. Project structure

    We implemented a Java bot starting from the Halite Java starter package.
The files that contain code written by our team are: MyBot.java, Location.java
and GameMap.java. Inside the Location class we added some methods that decide
if the current object (location) is neutral, belongs to the enemy or is outer.
We also added a method that returns the value of a location, based on its
strength, its production and the surrounding sites, as well as a method that
receives a list of other locations and returns the closest location from that
list. The only changes we brought to GameMap is that we added getters for its
Contents and Locations matrices. MyBot contains the main logic of our bot,
which is detailed below.

2. Implementation workflow

    We decided to keep track of the progress we made by saving multiple versions
of our bot. This way we could have them compete against each other and see if
the novelties that we introduced really made a difference or not. This was
especially effective on symmetrical boards, when the bots would make the same
decisions up to a certain point.

3. How the bot works

    First of all, when we receive the map from the environment at the start of
the match, before we send the init signal, we do a small analysis of the board,
calculating the average strength and production of the sites on the map. This
helps us determine the value of a neutral site and whether it is worth taking
or not.
    In each turn, we first determine the positions of the enemy and the neutral
sites. It is important to mention that when we search for neutral sites, we
take into consideration only those that have a greater "value" than the
average value of the map (which is the average production of the map divided by
the average strength). For a neutral site, its value is also the ratio between
its production and its strength (based on the fact that the production could be
considered as what the site has to "offer" and the strength is what we "lose"
when we conquer it).
    The way we make the moves is progressive, starting with the boundaries of the
territory we possess and going inwards. That is because we try to avoid as much
as possible the overlapping of pieces with great strength (since the strength is
limited at 255, and everything above this limit is lost). In order to do that, we
first look at the outer sites that we possess, we add their inner neighbors in a
queue and then we make the moves for those outer sites, then we continue with
the neighbors we just added and so on and so forth until the queue is empty, which
means that we moved all our pieces in the current turn.
    When we decide the direction where a piece should move, we first search for
the closest enemy and the closest neutral site. This is done using the
nearestLocation method in Location.java. Then we decide which of the two should
be chosen comparing their distances, but allowing the distance to the enemy to
be 1.5 larger than the distance to the neutral site (e.g: suppose the closest
enemy is at distance 8 and the closest neutral site is at distance 6, then we
would choose to go towards the enemy). This way we favor the attack/defense over
expansion. Finally, since there can be multiple directions that get us closer
to the enemy/neutral piece we chose, we try to pick the direction that goes to
a location with maximum value, while also trying to avoid excessive overlapping.
