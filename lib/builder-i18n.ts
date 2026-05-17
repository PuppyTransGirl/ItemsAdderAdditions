export interface BuilderTranslations {
    // Mode tabs
    tabActions: string;
    tabBehaviours: string;
    tabRecipes: string;
    tabWorldGen: string;
    // Badges
    badgeRequired: string;
    badgeOptional: string;
    selectDefault: string;
    // Buttons
    copy: string;
    copied: string;
    remove: string;
    addPotionEffect: string;
    addStep: string;
    addDisplay: string;
    addIngredient: string;
    // Section headers
    sectionParameters: string;
    sectionUniversalParameters: string;
    sectionFormat: string;
    sectionSteps: string;
    sectionMode: string;
    sectionDisplay: string;
    sectionDisplays: string;
    sectionRecipeInfo: string;
    sectionRecipeType: string;
    sectionCraftingGrid: string;
    sectionIngredients: string;
    sectionResult: string;
    sectionEntryInfo: string;
    sectionFilters: string;
    sectionPlacement: string;
    sectionGeneratedYaml: string;
    // Toggle / check labels
    toggleAddSound: string;
    toggleAdvancedOptions: string;
    toggleConfigureBlockFaces: string;
    toggleConfigurePotionEffects: string;
    toggleAddOpenSound: string;
    toggleAddCloseSound: string;
    // Field labels (prose, not raw YAML keys)
    labelSkillName: string;
    labelAnimationName: string;
    labelEmoteName: string;
    labelRadiusFormat: string;
    labelTextOneLine: string;
    labelItemsOneLine: string;
    labelBlockIdsOneLine: string;
    labelSlotsOneLine: string;
    labelDisplayId: string;
    labelIngredientItem: string;
    labelResultItem: string;
    labelResultAmount: string;
    labelRecipeId: string;
    labelEntryId: string;
    labelWorldsOneLine: string;
    labelBiomesOneLine: string;
    labelIngredientN: (n: number) => string;
    // Inline notes / description paragraphs
    noteReplaceBiome: string;
    noteConnectable: string;
    noteCraftingGrid: string;
    noteIngredientEmpty: string;
}

const en: BuilderTranslations = {
    tabActions: 'Actions',
    tabBehaviours: 'Behaviours',
    tabRecipes: 'Recipes',
    tabWorldGen: 'World Gen',
    badgeRequired: 'required',
    badgeOptional: 'optional',
    selectDefault: '(default)',
    copy: 'Copy',
    copied: 'Copied!',
    remove: 'Remove',
    addPotionEffect: '+ Add potion effect',
    addStep: '+ Add step',
    addDisplay: '+ Add display',
    addIngredient: '+ Add ingredient',
    sectionParameters: 'Parameters',
    sectionUniversalParameters: 'Universal parameters',
    sectionFormat: 'Format',
    sectionSteps: 'Steps',
    sectionMode: 'Mode',
    sectionDisplay: 'Display',
    sectionDisplays: 'Displays',
    sectionRecipeInfo: 'Recipe info',
    sectionRecipeType: 'Recipe type',
    sectionCraftingGrid: 'Crafting grid',
    sectionIngredients: 'Ingredients',
    sectionResult: 'Result',
    sectionEntryInfo: 'Entry info',
    sectionFilters: 'Filters',
    sectionPlacement: 'Placement',
    sectionGeneratedYaml: 'Generated YAML',
    toggleAddSound: 'Add sound',
    toggleAdvancedOptions: 'Advanced options',
    toggleConfigureBlockFaces: 'Configure block_faces',
    toggleConfigurePotionEffects: 'Configure potion effects',
    toggleAddOpenSound: 'Add open_sound',
    toggleAddCloseSound: 'Add close_sound',
    labelSkillName: 'skill name',
    labelAnimationName: 'animation name',
    labelEmoteName: 'emote name',
    labelRadiusFormat: 'radius format',
    labelTextOneLine: 'text (one per line)',
    labelItemsOneLine: 'items (one per line)',
    labelBlockIdsOneLine: 'block IDs (one per line)',
    labelSlotsOneLine: 'slots (one per line)',
    labelDisplayId: 'display id',
    labelIngredientItem: 'ingredient item',
    labelResultItem: 'result item',
    labelResultAmount: 'result amount',
    labelRecipeId: 'recipe id',
    labelEntryId: 'entry id',
    labelWorldsOneLine: 'worlds (one per line)',
    labelBiomesOneLine: 'biomes (one per line)',
    labelIngredientN: (n) => `ingredient ${n}`,
    noteReplaceBiome:
        'Note: Minecraft biome editing is not perfectly exact at small scales. Small shapes may look a bit randomized or rough.',
    noteConnectable:
        'All variant IDs are optional. If omitted: `default` uses the furniture this behaviour is on, every other type uses <furniture_name>_<type>.',
    noteCraftingGrid:
        'Type a letter in each cell. Cells with the same letter use the same ingredient. Empty cells become X in the pattern. X cannot be used as an ingredient key.',
    noteIngredientEmpty: 'Characters without an item defined are treated as empty slots.',
};

const fr: BuilderTranslations = {
    tabActions: 'Actions',
    tabBehaviours: 'Comportements',
    tabRecipes: 'Recettes',
    tabWorldGen: 'Monde',
    badgeRequired: 'requis',
    badgeOptional: 'optionnel',
    selectDefault: '(défaut)',
    copy: 'Copier',
    copied: 'Copié !',
    remove: 'Supprimer',
    addPotionEffect: '+ Ajouter un effet',
    addStep: '+ Ajouter une étape',
    addDisplay: '+ Ajouter un affichage',
    addIngredient: '+ Ajouter un ingrédient',
    sectionParameters: 'Paramètres',
    sectionUniversalParameters: 'Paramètres universels',
    sectionFormat: 'Format',
    sectionSteps: 'Étapes',
    sectionMode: 'Mode',
    sectionDisplay: 'Affichage',
    sectionDisplays: 'Affichages',
    sectionRecipeInfo: 'Informations sur la recette',
    sectionRecipeType: 'Type de recette',
    sectionCraftingGrid: 'Grille de fabrication',
    sectionIngredients: 'Ingrédients',
    sectionResult: 'Résultat',
    sectionEntryInfo: 'Informations sur l\'entrée',
    sectionFilters: 'Filtres',
    sectionPlacement: 'Placement',
    sectionGeneratedYaml: 'YAML généré',
    toggleAddSound: 'Ajouter un son',
    toggleAdvancedOptions: 'Options avancées',
    toggleConfigureBlockFaces: 'Configurer block_faces',
    toggleConfigurePotionEffects: 'Configurer les effets de potion',
    toggleAddOpenSound: 'Ajouter open_sound',
    toggleAddCloseSound: 'Ajouter close_sound',
    labelSkillName: 'nom de la compétence',
    labelAnimationName: 'nom de l\'animation',
    labelEmoteName: 'nom de l\'emote',
    labelRadiusFormat: 'format du rayon',
    labelTextOneLine: 'texte (un par ligne)',
    labelItemsOneLine: 'objets (un par ligne)',
    labelBlockIdsOneLine: 'IDs de blocs (un par ligne)',
    labelSlotsOneLine: 'emplacements (un par ligne)',
    labelDisplayId: 'identifiant de l\'affichage',
    labelIngredientItem: 'objet ingrédient',
    labelResultItem: 'objet résultat',
    labelResultAmount: 'quantité résultat',
    labelRecipeId: 'identifiant de recette',
    labelEntryId: 'identifiant de l\'entrée',
    labelWorldsOneLine: 'mondes (un par ligne)',
    labelBiomesOneLine: 'biomes (un par ligne)',
    labelIngredientN: (n) => `ingrédient ${n}`,
    noteReplaceBiome:
        'Note : l\'édition des biomes Minecraft n\'est pas parfaitement précise à petite échelle. Les petites formes peuvent sembler un peu aléatoires ou rugueuses.',
    noteConnectable:
        'Tous les IDs de variante sont optionnels. Si omis : `default` utilise le meuble sur lequel le comportement est configuré, les autres types utilisent <nom_du_meuble>_<type>.',
    noteCraftingGrid:
        'Entrez une lettre dans chaque cellule. Les cellules avec la même lettre utilisent le même ingrédient. Les cellules vides deviennent X dans le patron. X ne peut pas être utilisé comme clé d\'ingrédient.',
    noteIngredientEmpty: 'Les caractères sans objet défini sont traités comme des emplacements vides.',
};

const nl: BuilderTranslations = {
    tabActions: 'Acties',
    tabBehaviours: 'Gedragingen',
    tabRecipes: 'Recepten',
    tabWorldGen: 'Wereld',
    badgeRequired: 'verplicht',
    badgeOptional: 'optioneel',
    selectDefault: '(standaard)',
    copy: 'Kopiëren',
    copied: 'Gekopieerd!',
    remove: 'Verwijderen',
    addPotionEffect: '+ Effect toevoegen',
    addStep: '+ Stap toevoegen',
    addDisplay: '+ Weergave toevoegen',
    addIngredient: '+ Ingrediënt toevoegen',
    sectionParameters: 'Parameters',
    sectionUniversalParameters: 'Universele parameters',
    sectionFormat: 'Formaat',
    sectionSteps: 'Stappen',
    sectionMode: 'Modus',
    sectionDisplay: 'Weergave',
    sectionDisplays: 'Weergaven',
    sectionRecipeInfo: 'Receptinformatie',
    sectionRecipeType: 'Recepttype',
    sectionCraftingGrid: 'Knutselraster',
    sectionIngredients: 'Ingrediënten',
    sectionResult: 'Resultaat',
    sectionEntryInfo: 'Invoergegevens',
    sectionFilters: 'Filters',
    sectionPlacement: 'Plaatsing',
    sectionGeneratedYaml: 'Gegenereerde YAML',
    toggleAddSound: 'Geluid toevoegen',
    toggleAdvancedOptions: 'Geavanceerde opties',
    toggleConfigureBlockFaces: 'block_faces instellen',
    toggleConfigurePotionEffects: 'Potio-effecten instellen',
    toggleAddOpenSound: 'open_sound toevoegen',
    toggleAddCloseSound: 'close_sound toevoegen',
    labelSkillName: 'vaardigheidsnaam',
    labelAnimationName: 'animatienaam',
    labelEmoteName: 'emotenaam',
    labelRadiusFormat: 'straalformaat',
    labelTextOneLine: 'tekst (een per regel)',
    labelItemsOneLine: 'items (een per regel)',
    labelBlockIdsOneLine: 'blok-ID\'s (een per regel)',
    labelSlotsOneLine: 'slots (een per regel)',
    labelDisplayId: 'weergave-ID',
    labelIngredientItem: 'ingredientitem',
    labelResultItem: 'resultaatitem',
    labelResultAmount: 'resultaathoeveelheid',
    labelRecipeId: 'recept-ID',
    labelEntryId: 'invoer-ID',
    labelWorldsOneLine: 'werelden (een per regel)',
    labelBiomesOneLine: 'biomen (een per regel)',
    labelIngredientN: (n) => `ingrediënt ${n}`,
    noteReplaceBiome:
        'Let op: Minecraft bioombewerking is niet perfect precies op kleine schaal. Kleine vormen kunnen er iets willekeurig of ruw uitzien.',
    noteConnectable:
        'Alle variant-ID\'s zijn optioneel. Indien weggelaten: `default` gebruikt het meubel waarop dit gedrag is geconfigureerd, alle andere typen gebruiken <meubelnaam>_<type>.',
    noteCraftingGrid:
        'Typ een letter in elke cel. Cellen met dezelfde letter gebruiken hetzelfde ingrediënt. Lege cellen worden X in het patroon. X kan niet worden gebruikt als ingredientsleutel.',
    noteIngredientEmpty: 'Tekens zonder gedefinieerd item worden behandeld als lege slots.',
};

export const builderTranslations: Record<string, BuilderTranslations> = {en, fr, nl};
