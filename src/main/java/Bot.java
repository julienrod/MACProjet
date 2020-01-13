//Sources : https://monsterdeveloper.gitbooks.io/writing-telegram-bots-on-java/content/

import org.bson.Document;
import org.bson.types.ObjectId;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.StrictMath.toIntExact;

public class Bot extends TelegramLongPollingBot {
    private HashMap<Long, Integer> addRecipeStatus = new HashMap<>();
    private HashMap<Long, List<List<String>>> addRecipeData = new HashMap<>();

    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            long userId = update.getMessage().getChat().getId();
            String userFirstNname = update.getMessage().getChat().getFirstName();
            String userLastName = update.getMessage().getChat().getLastName();
            String userUsername = update.getMessage().getChat().getUserName();
            String message_text = update.getMessage().getText();
            MongoDBDAO.getInstance().check(userFirstNname, userLastName, toIntExact(userId), userUsername);
            long chat_id = update.getMessage().getChatId();
            SendMessage message = null;

            if (message_text.equals("/reset")) {
                addRecipeData.remove(userId);
                addRecipeStatus.remove(userId);
                message = new SendMessage( ).setChatId(chat_id).setText("L'ajout de recette a été avorté");
            }else if(addRecipeStatus.containsKey(userId)) {
                switch (addRecipeStatus.get(userId)) {
                    case 0:
                        newRecipeList(userId, message_text);
                        message = new SendMessage().setChatId(chat_id).setText("Veuillez spécifier les ustenciles (" +
                                "séparés par une virgule)\n Exemple: mixer, micro-ondes, spatule");
                        break;
                    case 1:
                        newRecipeList(userId, message_text);
                        message = new SendMessage().setChatId(chat_id).setText("Veuillez spécifier le temps de" +
                                " préparation\n Exemple: 1h 30m");
                        break;
                    case 2:
                        if(!newRecipeRegex(userId, message_text, "([0-9]+m|[0-9]+h|[0-9]+h [0-9]+m)")) {
                            message = new SendMessage().setChatId(chat_id).setText("Format incorrect\n Exemple: 1h 30m");
                        }else{
                            message = new SendMessage().setChatId(chat_id).setText("Veuillez spécifier le nombre de" +
                                    " calories\n Exemple: 250kcal");
                        }
                        break;
                    case 3:
                        if(!newRecipeRegex(userId, message_text, "([0-9]+(kcal)?)")) {
                            message = new SendMessage().setChatId(chat_id).setText("Format incorrect\n Exemple: 250kcal");
                        }else{
                            message = new SendMessage().setChatId(chat_id).setText("Veuillez décrire la recette");
                        }
                        break;
                    case 4:
                        newRecipeSinglePhrase(userId, message_text);
                        message = new SendMessage().setChatId(chat_id).setText("Vous pouvez aussi spécifier d'autres " +
                                "tags\n Exemple: biscuit, gateau, pâtisserie japonaise, etc...");
                        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        rowInline.add(new InlineKeyboardButton().setText("Non merci").setCallbackData("nothankyou " + userId));
                        rowsInline.add(rowInline);
                        markupInline.setKeyboard(rowsInline);
                        message.setReplyMarkup(markupInline);
                        break;
                    case 5:
                        newRecipeList(userId, message_text);
                        addNewRecipe(userId);
                        message = new SendMessage( ).setChatId(chat_id).setText("La recette a été ajoutée");
                        break;
                }
            }else if (message_text.startsWith("/newrecipe ")) {
                newRecipeSinglePhrase(userId, message_text.substring(11));
                message = new SendMessage().setChatId(chat_id).setText("Veuillez spécifier les ingrédients avec " +
                                "leur quantité (séparer les quantités des ingrédients avec '/')\n " +
                                "Exemple: sucre/250g, farine/150g, eau/2 tasses, confiture d'abricot/1 pot");
            }else if (message_text.equals("/random")) {
            }else if (message_text.startsWith("/getrecipe ")) {
                message = new SendMessage( ).setChatId(chat_id).setText(
                        getReceipeById(message_text.substring(11)));
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this receipe").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/recipesbyingredients ")) {
                List<String> ingredients = Arrays.asList(message_text.substring(22).replaceAll(" ", "").split(","));
                message = new SendMessage( ).setChatId(chat_id).setText("With " + message_text.substring(22) + "\n" +
                        getReceipesByIngredients(ingredients));
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like these ingredients").setCallbackData(
                        "Like " + userId  + " " + message_text.substring(22)));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/recipesbyuser ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this user").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/recipesbycalory ")) {
            }else if (message_text.startsWith("/recipesbymachine ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this machine").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/recipesbytime ")) {
            }else if (message_text.startsWith("/showrecipe ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this recipe").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.equals("/userscooking")) {
            }else if (message_text.startsWith("/recommendations")) {
            }else if (message_text.equals("/help")) {
                message = new SendMessage().setChatId(chat_id).setText(
                    "/newrecipe [nom] -> Démarre la création de la recette [nom]\n" +
                    "/reset -> Met fin au processus de création d'une recette\n" +
                    "/random -> Affiche une recette aléatoire\n" +
                    "/getrecipe [nom] -> Affiche les recettes ayant pour nom [nom]\n" +
                    "/recipesbyingredients [i1, i2, ...] -> Affiche les recettes ayant pour ingrédients [i1, i2, ...]\n" +
                    "/recipesbyuser [utilisateur] -> Affiche les recettes de l'utilisateur [utilisateur]\n" +
                    "/recipesbycalory [calories] -> Affiche les recettes ayant moins de [calories] calories\n" +
                    "/recipesbymachine [m1, m2, ...] -> Affiche les recettes ayant pour ustensiles [m1, m2, ...]\n" +
                    "/recipesbytime [temps] -> Affiche les recettes prenant [temps] à réaliser à 5 minutes près\n" +
                    "/showrecipe [recette] -> Affiche la recette [recette]\n" +
                    "/userscooking -> Affiche les utilisateurs en train de cuisiner\n" +
                    "/recommendations -> Propose des recettes sur la base de celles consultées jusqu'à présent\n" +
                    "/help -> Affiche la liste des commandes disponibles");
            }else{
                message = new SendMessage().setChatId(chat_id).setText(message_text);
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Update message text").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }else if (update.hasCallbackQuery()) {
            //A utiliser pour rajouter des like et nothankyou
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            if(call_data.startsWith("nothankyou ")){
                String userId = call_data.substring(11);
                addNewRecipe(Long.parseLong(userId));
                SendMessage message = new SendMessage( ).setChatId(chat_id).setText("La recette a été ajoutée");
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if (call_data.startsWith("Like ")) {
               List<String> liked = Arrays.asList(call_data.substring(5).split(" "));
               Neo4jDAO.getInstance().addLike(liked);
            }
        }
    }

    public String getBotUsername() {
        return "SyugarDaddyBot";
    }

    public String getBotToken() {
        return "1057276327:AAHJLLnJ-4kCdQWe5hCnFtJB5V8ZGf7FTwY";
    }

    private void newRecipeSinglePhrase(long id, String recipeName) {
        int state = addRecipeStatus.get(id) == null ? -1 : addRecipeStatus.get(id);
        List<List<String>> newRecipe = new ArrayList<>();
        List<String> name = new ArrayList<>();
        name.add(recipeName);
        if(addRecipeStatus.get(id) != null) {
            newRecipe = addRecipeData.get(id);
        }
        newRecipe.add(name);
        addRecipeData.put(id, newRecipe);
        addRecipeStatus.put(id, ++state);

    }

    private void newRecipeList(long id, String list) {
        int state = addRecipeStatus.get(id);
        List<String> myList = Arrays.asList(list.replace("[^a-zA-Z/]+", "")
                .toLowerCase().replace(" ", "").split(","));
        List<List<String>> newRecipe = addRecipeData.get(id);
        newRecipe.add(myList);
        addRecipeData.put(id, newRecipe);
        addRecipeStatus.put(id, ++state);
    }

    private boolean newRecipeRegex(long id, String list, String regex) {
        int state = addRecipeStatus.get(id);
        Pattern pat = Pattern.compile(regex);
        Matcher match = pat.matcher(list);
        if(!match.find()) return false;
        List<String> myList = Arrays.asList(list.toLowerCase());
        List<List<String>> newRecipe = addRecipeData.get(id);
        newRecipe.add(myList);
        addRecipeData.put(id, newRecipe);
        addRecipeStatus.put(id, ++state);
        return true;
    }

    private void addNewRecipe(long id) {
        //Nom de la recette, ingrédients, ustenciles, temps, kcal, description, sous-catégorie
        String name = addRecipeData.get(id).get(0).get(0);
        String description = addRecipeData.get(id).get(5).get(0);
        String time = addRecipeData.get(id).get(3).get(0);
        String kcal = addRecipeData.get(id).get(4).get(0);
        List<String> ingredients = addRecipeData.get(id).get(1);
        List<String> ustenciles = addRecipeData.get(id).get(2);
        List<String> subcategories = null;
        if (addRecipeData.get(id).size() > 6) {
            subcategories = addRecipeData.get(id).get(6);
        }else{
            subcategories = new LinkedList<>();
        }
        ObjectId recipeId = MongoDBDAO.getInstance().addRecipe(name, description, time, kcal);
        Neo4jDAO.getInstance().addRecipe(id, recipeId.toString(), ingredients, ustenciles, subcategories);
    }

    private String getReceipesByIngredients(List<String> ingredients){
        String result = "";
        StatementResult str = Neo4jDAO.getInstance().getRecipeByIngredients(ingredients);
        while ( str.hasNext() )
        {
            Record record = str.next();
            String recipeId = record.get(0).asString().substring(1);
            Document recipe = MongoDBDAO.getInstance().findDocument(recipeId);
            result += recipeId + "\t\t" + recipe.get("name") + "\n";
        }
        if(result.equals("")){
            result += "No result found";
        }else{
            result = "id \t\t\t\t\t\t\t\t\t nom\n" + result;
        }
        return result;
    }

    private String getReceipeById(String recipeId){
        String recipe = "";
        Document documentation = MongoDBDAO.getInstance().findDocument(recipeId);
        StatementResult ingredients = Neo4jDAO.getInstance().getRecipeParts(recipeId, "IN", ",rel.quantite");
        StatementResult machines = Neo4jDAO.getInstance().getRecipeParts(recipeId, "USEFULL", "");
        return recipe;
    }
}
